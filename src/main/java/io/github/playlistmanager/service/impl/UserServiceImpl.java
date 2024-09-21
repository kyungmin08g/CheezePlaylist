package io.github.playlistmanager.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.mapper.UserMapper;
import io.github.playlistmanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final String youtubeApiKey;
    private final UserMapper userMapper;

    public UserServiceImpl(@Value("${youtube.api-key}") String youtubeApiKey, UserMapper userMapper) {
        this.youtubeApiKey = youtubeApiKey;
        this.userMapper = userMapper;
    }

    public void musicDownload(String artist, String title) {
        String query = artist + " - " + title; // 하나의 문자열로 만듬
        String searchVideoResultJson = searchVideo(query); // 만든 문자열을 담아 유튜브 동영상을 검색함

        try {
            String videoIdValue = "";
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(searchVideoResultJson); // Json을 Tree 형태로 바꿔줌
            JsonNode items = json.get("items"); // 해당 트리의 하위 배열을 가져옴

            for (JsonNode item : items) {
                JsonNode id = item.get("id"); // 해당 트리의 하위 배열을 가져옴
                JsonNode videoId = id.get("videoId"); // 해당 트리의 값을 가져옴
                videoIdValue = videoId.asText(); // 텍스트(문자열)로 바꿈
            }

            String url = "https://www.youtube.com/watch?v=" + videoIdValue; // 유튜브 영상의 주소를 만듬
            downloadMusicFile(url, title); // URL과 해당 음악의 제목을 넣으면 음악 byte[]를 DB에 저장함

            log.info("해당 음악을 다운로드하고 DB에 저장하는 로직이 정상적으로 처리되었습니다.");
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    // 쿼리를 받으면 YouTube Data API를 통해 동영상을 검색하는 메소드
    public String searchVideo(String query) {
        Mono<String> response = WebClient.builder().baseUrl("https://www.googleapis.com").build().get()
                .uri(uriBuilder -> uriBuilder.path("/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query + " (lyrics)")
                        .queryParam("maxResults", "1")
                        .queryParam("type", "video")
                        .queryParam("key", youtubeApiKey).build())
                .retrieve()
                .bodyToMono(String.class);

        log.info("유튜브 데이터 API를 통해서 동영상을 검색하는 로직이 정상적으로 처리되었습니다.");
        return response.block(); // Json으로 리턴함
    }

    public byte[] mp3Conversion(String youtubeUrl) throws IOException, InterruptedException {
        System.out.println("Starting download from URL: " + youtubeUrl);

        // yt-dlp 프로세스 생성
        ProcessBuilder ytDlpBuilder = new ProcessBuilder("yt-dlp", "-f", "bestaudio", "-o", "-", youtubeUrl);
        Process ytDlpProcess = ytDlpBuilder.start();
        System.out.println("yt-dlp process started.");

        // ffmpeg 프로세스 생성
        ProcessBuilder ffmpegBuilder = new ProcessBuilder("ffmpeg", "-i", "pipe:0", "-f", "mp3", "pipe:1");
        Process ffmpegProcess = ffmpegBuilder.start();
        System.out.println("ffmpeg process started.");

        // yt-dlp의 출력을 ffmpeg의 입력으로 연결
        try (InputStream ytDlpOutput = ytDlpProcess.getInputStream();
             InputStream ffmpegInput = ffmpegProcess.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // yt-dlp의 출력을 ffmpeg에 연결
            Thread ytDlpToFfmpeg = new Thread(() -> {
                try {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = ytDlpOutput.read(buffer)) != -1) {
                        ffmpegProcess.getOutputStream().write(buffer, 0, bytesRead);
                    }
                    ffmpegProcess.getOutputStream().close();
                    System.out.println("yt-dlp output sent to ffmpeg.");
                } catch (IOException e) {
                    System.err.println("Error in yt-dlp to ffmpeg thread: " + e.getMessage());
                }
            });

            ytDlpToFfmpeg.start();

            // ffmpeg의 출력을 byte array로 읽기
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = ffmpegInput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("ffmpeg output read successfully.");

            ytDlpToFfmpeg.join(); // yt-dlp 쓰레드가 끝날 때까지 기다리기
            System.out.println("yt-dlp thread finished.");

            return outputStream.toByteArray();
        } catch (IOException e) {
            System.err.println("Error during audio processing: " + e.getMessage());
            throw e; // 다시 예외를 던져서 컨트롤러에서 처리할 수 있도록 함
        } finally {
            ytDlpProcess.waitFor();
            ffmpegProcess.waitFor();
            System.out.println("yt-dlp and ffmpeg processes finished.");
        }
    }

    private String extractFileName(String output) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("has already been downloaded")) {
                // "has already been downloaded" 라인에서 파일 이름 추출
                String[] parts = line.split(" ");
                // 마지막 두 번째 요소가 파일 이름일 가능성이 높음
                return parts[parts.length - 5]; // 파일 이름
            }
        }
        throw new RuntimeException("다운로드된 파일 이름을 찾을 수 없습니다.");
    }

    public void downloadMusicFile(String youtubeURL, String title) throws IOException, InterruptedException {
        // DB에서 Title을 통해 조회하게 되는데 결과가 null인 경우에는 로그만 찍고 아래 로직을 실행하도록하는 검사 작업 -> 데이터가 존재하지 않으면 추가하는게 맞으니까
        String musicFileDTO1 = Optional.ofNullable(findByTitle(title)).map(MusicFileDTO::getName).orElse("Untitled");
        if (musicFileDTO1.equals("Untitled")) {
            if (mp3Conversion(youtubeURL) == null) {
                log.info("URL에 대한 데이터가 없음");
                return;
            }

            byte[] data = mp3Conversion(youtubeURL);
            System.out.println(Arrays.toString(data));
            MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                    .name(title + ".mp3")
                    .data(data)
                    .build();

            mp3FileSave(musicFileDTO);
            log.info("DB에 저장 성공!");
        } else {
            log.info("이미 해당 파일이 있음");
        }
    }

    @Override
    public void mp3FileSave(MusicFileDTO musicFileDTO) {
        userMapper.mp3FileSave(musicFileDTO);
    }

    @Override
    public MusicFileDTO findByTitle(String title) {
        return userMapper.findByTitle(title);
    }

    @Override
    public void signUp(JoinMemberDTO joinMemberDTO) {
        userMapper.signUp(joinMemberDTO);
    }

    @Override
    public JoinMemberDTO selectMemberByUsername(String username) {
        return userMapper.selectMemberByUsername(username);
    }

}
