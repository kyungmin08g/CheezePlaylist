package io.github.playlistmanager.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.mapper.UserMapper;
import io.github.playlistmanager.provider.RedisProvider;
import io.github.playlistmanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final String youtubeApiKey;
    private final UserMapper userMapper;
    private final RedisProvider redisProvider;

    public UserServiceImpl(@Value("${youtube.api-key}") String youtubeApiKey, UserMapper userMapper, RedisProvider redisProvider) {
        this.youtubeApiKey = youtubeApiKey;
        this.userMapper = userMapper;
        this.redisProvider = redisProvider;
    }

    public void musicDownload(int roomId, String artist, String title) {
        String query = artist + " - " + title; // 하나의 문자열로 만듬
        String customTitle = title.replace(" ", "_");

        String optionalMusicFileDTO = Optional.ofNullable(findByTitle(roomId, customTitle)).map(MusicFileDTO::getTitle).orElse("Untitled");

        // 검색 API 호출 전에 해당 음악이 있는지 체크
        if (!optionalMusicFileDTO.equals("Untitled")) {
            log.info("해당 음악이 이미 있음");
            return;
        }

        MusicFileDTO optionalTitleDTO = selectMusicFilesByTitle(customTitle);

        if (optionalTitleDTO != null) {
            MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                    .roomId(roomId)
                    .title(customTitle)
                    .musicFileBytes(optionalTitleDTO.getMusicFileBytes())
                    .build();

            mp3FileSave(musicFileDTO);
            log.info("DB에 음악이 있으므로 검색하지 않고 저장함.");
            return;
        }

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
            downloadMusicFile(roomId, url, customTitle); // URL과 해당 음악의 제목을 넣으면 음악 byte[]를 DB에 저장함

            log.info("해당 음악을 다운로드하고 DB에 저장하는 로직이 정상적으로 처리되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 쿼리를 받으면 YouTube Data API를 통해 동영상을 검색하는 메소드임
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
        log.info("유튜브 영상의 주소: {}", youtubeUrl);

        // yt-dlp 프로세스 생성
        ProcessBuilder ytDlpBuilder = new ProcessBuilder("yt-dlp", "-f", "bestaudio", "-o", "-", youtubeUrl);
        Process ytDlpProcess = ytDlpBuilder.start();
        log.info("yt-dlp 프로세스가 시작되었음");

        // ffmpeg 프로세스 생성
        ProcessBuilder ffmpegBuilder = new ProcessBuilder("ffmpeg", "-i", "pipe:0", "-f", "mp3", "pipe:1");
        Process ffmpegProcess = ffmpegBuilder.start();
        log.info("ffmpeg 프로세스가 시작되었음");

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
                    log.info("yt-dlp 출력이 ffmpeg로 전송됨");
                } catch (IOException e) {
                    log.error("ffmpeg 스레드에 대한 yt-dlp 오류: {}", e.getMessage());
                }
            });

            ytDlpToFfmpeg.start();

            // ffmpeg의 출력을 byte array로 읽기
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = ffmpegInput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            log.info("ffmpeg 출력을 성공적으로 읽음");

            ytDlpToFfmpeg.join(); // yt-dlp 쓰레드가 끝날 때까지 기다리기
            log.info("yt-dlp 스레드가 완료됨");

            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("오디오 처리 중 오류 발생: {}", e.getMessage());
            throw e; // 다시 예외를 던져서 컨트롤러에서 처리할 수 있도록 함
        } finally {
            ytDlpProcess.waitFor();
            ffmpegProcess.waitFor();
            log.info("yt-dlp 및 ffmpeg 프로세스가 완료됨");
        }
    }

    public void downloadMusicFile(int roomId, String youtubeURL, String customTitle) throws IOException, InterruptedException {
        byte[] mp3Data = mp3Conversion(youtubeURL);

        // DB에서 Title을 통해 조회하게 되는데 결과가 null인 경우에는 로그만 찍고 아래 로직을 실행하도록하는 검사 작업 -> 데이터가 존재하지 않으면 추가하는게 맞으니까
        if (mp3Data == null) {
            log.info("URL에 대한 데이터가 없음");
        } else {
            // MusicFileDTO 객체 생성
            MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                    .roomId(roomId)
                    .title(customTitle)
                    .musicFileBytes(mp3Data)
                    .build();

            mp3FileSave(musicFileDTO);
            log.info("DB에 저장 성공!");
        }
    }

    @Override
    public void mp3FileSave(MusicFileDTO musicFileDTO) {
        userMapper.mp3FileSave(musicFileDTO);
    }

    @Override
    public MusicFileDTO findByTitle(int roomId, String title) {
        return userMapper.findByTitle(roomId, title);
    }

    @Override
    public List<MusicFileDTO> selectMusicFiles(int roomId) {
        return userMapper.selectMusicFiles(roomId);
    }

    @Override
    public void signUp(JoinMemberDTO joinMemberDTO) {
        userMapper.signUp(joinMemberDTO);
    }

    @Override
    public JoinMemberDTO selectMemberByUsername(String username) {
        return userMapper.selectMemberByUsername(username);
    }

    @Override
    public MusicFileDTO selectMusicFilesByTitle(String title) {
        return userMapper.selectMusicFilesByTitle(title);
    }

    @Override
    public void deleteMusicFile(int roomId, String title) {
        userMapper.deleteMusicFile(roomId, title);
    }

}
