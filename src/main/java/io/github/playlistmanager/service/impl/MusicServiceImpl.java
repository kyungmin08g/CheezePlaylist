package io.github.playlistmanager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chzzkapi.api.ChzzkAPI;
import io.github.playlistmanager.dto.ChzzkChannelConnectDto;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.dto.PlaylistDto;
import io.github.playlistmanager.mapper.MusicMapper;
import io.github.playlistmanager.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MusicServiceImpl implements MusicService {

    private final String key;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MusicMapper musicMapper;
    private final ObjectMapper objectMapper;
    private final ChzzkAPI chzzkAPI;

    public MusicServiceImpl(
            @Value("${youtube.api-key}") String key,
            SimpMessagingTemplate simpMessagingTemplate,
            MusicMapper musicMapper,
            ChzzkAPI chzzkAPI
    ) {
        this.key = key;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.musicMapper = musicMapper;
        this.objectMapper = new ObjectMapper();
        this.chzzkAPI = chzzkAPI;
    }

    @Override
    public void memberMusicDownload(String roomId, String artist, String title) {
        musicDownload(roomId, artist, title);

        String customTitle = title.replace(" ", "_");
        MusicFileDTO dto = musicMapper.findByMusic(roomId, artist, customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                .artist(artist)
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        simpMessagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }

    @Override
    public void donationMusicDownload(String roomId, String donationContent) {
        String artist;
        String title;

        if(donationContent.matches("^(.*) - (.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 1);
            title = donationContent.substring(donationContent.indexOf("-") + 2);
            memberMusicDownload(roomId, artist, title);
        } else if (donationContent.matches("^(.*) -(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 1);
            title = donationContent.substring(donationContent.indexOf("-") + 1);
            memberMusicDownload(roomId, artist, title);
        } else if (donationContent.matches("^(.*)- (.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-"));
            title = donationContent.substring(donationContent.indexOf("-") + 2);
            memberMusicDownload(roomId, artist, title);
        } else if (donationContent.matches("^(.*)-(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-"));
            title = donationContent.substring(donationContent.indexOf("-") + 1);
            memberMusicDownload(roomId, artist, title);
        } else if (donationContent.matches("^(.*) - {2}(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 1);
            title = donationContent.substring(donationContent.indexOf("-") + 3);
            memberMusicDownload(roomId, artist, title);
        } else if (donationContent.matches("^(.*) {2}- (.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 2);
            title = donationContent.substring(donationContent.indexOf("-") + 2);
            memberMusicDownload(roomId, artist, title);
        } else if (donationContent.matches("^(.*) {2}- {2}(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 2);
            title = donationContent.substring(donationContent.indexOf("-") + 3);
            memberMusicDownload(roomId, artist, title);
        }
    }

    @Override
    public String calculateMonthsSubscribed(JsonNode streamingPropertyNode) {
        JsonNode subscription = streamingPropertyNode.get("subscription");
        if (subscription == null) return null;

        JsonNode accumulativeMonth = subscription.get("accumulativeMonth");

        int year = 0;
        int month = 0;
        for (int i = 12; i <= accumulativeMonth.asInt(); i += 12) {
            year += 1;
            month = i;
        }

        if (year == 0) return accumulativeMonth.asInt() + "개월";
        if ((accumulativeMonth.asInt() - month) == 0) return year + "년";

        return year + "년 " + (accumulativeMonth.asInt() - month) + "개월";
    }

    @Override
    public void donationChat(String roomId, String chatJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(chatJson);
            JsonNode bdyJson = jsonNode.get("bdy");
            if (bdyJson == null) return;

            JsonNode auth = bdyJson.get("auth"); // 처음에 연결할떄 생기는 JSON
            if (auth != null) return;

            for (JsonNode bdyNode : bdyJson) {
                JsonNode msgTypeCode = bdyNode.get("msgTypeCode");
                if (msgTypeCode == null) return;

                if (msgTypeCode.asInt() == 10) { // 후원 타입이면..
                    String profile = bdyNode.get("profile").asText();
                    String payAmount = objectMapper.readTree(bdyNode.get("extras").asText()).get("payAmount").asText();
                    JsonNode message = bdyNode.get("msg");

                    if (profile.equals("null")) { // 프로필이 없을 떄
                        System.out.println("\u001B[33m[후원] 익명: " + message.asText() + " [" + payAmount + "원]\u001B[0m");
                        donationMusicDownload(roomId, message.asText());
                    } else {
                        JsonNode profileNode = objectMapper.readTree(profile);
                        JsonNode nickname = profileNode.get("nickname");
                        JsonNode streamingProperty = profileNode.get("streamingProperty"); // 구독

                        String subscriptionMonth = calculateMonthsSubscribed(streamingProperty);
                        if (subscriptionMonth != null) {
                            System.out.println("\u001B[33m[후원] " + nickname.asText() + ": " + message.asText() + " [" + payAmount + "원]" + " [" + subscriptionMonth + " 구독 중]" + "\u001B[0m");
                            donationMusicDownload(roomId, message.asText());
                        } else {
                            System.out.println("\u001B[33m[후원] " + nickname.asText() + ": " + message.asText() + " [" + payAmount + "원]" + "\u001B[0m");
                            donationMusicDownload(roomId, message.asText());
                        }
                    }

                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    // 쿼리를 받으면 YouTube Data API를 통해 동영상을 검색하는 메소드임
    @Override
    public String searchVideo(String query) {
        System.out.println("어떻게 검색하는지 궁금해서: " + query);
        Mono<String> response = WebClient.builder().baseUrl("https://www.googleapis.com").build().get()
                .uri(uriBuilder -> uriBuilder.path("/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query + " (lyrics)")
                        .queryParam("maxResults", "1")
                        .queryParam("type", "video")
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(String.class);

        log.info("유튜브 데이터 API를 통해서 동영상을 검색하는 로직이 정상적으로 처리되었습니다.");
        return response.block(); // Json으로 리턴함
    }

    @Override
    public void musicDownload(String roomId, String artist, String title) {
        String query = artist + " - " + title; // 하나의 문자열로 만듬
        String customTitle = title.replace(" ", "_");

        // 검색 API 호출 전에 해당 음악이 있는지 체크
        String optionalMusicFileDTO = Optional.ofNullable(findByMusic(roomId, artist, customTitle)).map(MusicFileDTO::getTitle).orElse("Untitled");
        if (!optionalMusicFileDTO.equals("Untitled")) {
            log.info("해당 음악이 이미 있음");
            return;
        }

        // 다운로드 휫수를 줄이기 위해 해당음악이 있으면 가져와 저장
        MusicFileDTO optionalTitleDTO = findByArtistAndTitle(artist, customTitle);
        if (optionalTitleDTO != null) {
            MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                    .artist(artist)
                    .roomId(roomId)
                    .title(customTitle)
                    .musicFileBytes(optionalTitleDTO.getMusicFileBytes())
                    .build();

            save(musicFileDTO);
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
            conversionAndDownload(roomId, url, artist, customTitle); // URL과 해당 음악의 제목을 넣으면 음악 byte[]를 DB에 저장함

            log.info("해당 음악을 다운로드하고 DB에 저장하는 로직이 정상적으로 처리되었습니다.");
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    @Override
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

    @Override
    public void conversionAndDownload(String roomId, String youtubeURL, String artist, String customTitle) throws IOException, InterruptedException {
        byte[] mp3Data = mp3Conversion(youtubeURL);

        // DB에서 Title을 통해 조회하게 되는데 결과가 null인 경우에는 로그만 찍고 아래 로직을 실행하도록하는 검사 작업 -> 데이터가 존재하지 않으면 추가하는게 맞으니까
        if (mp3Data == null) {
            log.info("URL에 대한 데이터가 없음");
        } else {
            // MusicFileDTO 객체 생성
            MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                    .roomId(roomId)
                    .artist(artist)
                    .title(customTitle)
                    .musicFileBytes(Base64.getEncoder().encodeToString(mp3Data))
                    .build();

            save(musicFileDTO);
            log.info("DB에 저장 성공!");
        }
    }

    @Override
    public ChzzkChannelConnectDto chzzkChannelConnect(PlaylistDto playlistDto) {
        String channelId = playlistDto.getChzzkChannelId();
        String channelName = chzzkAPI.getChannelName(channelId);
        String chatChannelId = chzzkAPI.getChatChannelId(channelId);
        String accessToken = chzzkAPI.getAccessToken(chatChannelId);

        int serverId = 0;
        for (char i : chatChannelId.toCharArray()) {
            serverId += Character.getNumericValue(i);
        }
        serverId = Math.abs(serverId) % 9 + 1;

        ChzzkChannelConnectDto connectDto = ChzzkChannelConnectDto.builder()
                .playlistId(playlistDto.getPlaylistId())
                .chatChannelId(chatChannelId)
                .accessToken(accessToken)
                .serverId(String.valueOf(serverId))
                .build();

        log.info("\u001B[32m" + channelName + "님 채널에 연결하는 로직이 정상적으로 처리되었습니다.\u001B[0m");
        return connectDto;
    }

    @Override
    public void save(MusicFileDTO musicFileDTO) {
        musicMapper.save(musicFileDTO);
    }

    @Override
    public MusicFileDTO findByMusic(String roomId, String artist, String title) {
        return musicMapper.findByMusic(roomId, artist, title);
    }

    @Override
    public List<MusicFileDTO> findById(String roomId) {
        return musicMapper.findById(roomId);
    }

    @Override
    public MusicFileDTO findByArtistAndTitle(String artist, String title) {
        return musicMapper.findByArtistAndTitle(artist, title);
    }

    @Override
    public void delete(String roomId, String artist, String title) {
        musicMapper.delete(roomId, artist, title);
    }

    @Override
    public void deleteById(String playlistId) {
        musicMapper.deleteById(playlistId);
    }

    @Override
    public void saveChannelId(PlaylistDto dto) {
        musicMapper.saveChannelId(dto);
    }

    @Override
    public PlaylistDto findByIdAndPlaylistName(String playlistId, String playlistName, String username) {
        return musicMapper.findByIdAndPlaylistName(playlistId, playlistName, username);
    }

    @Override
    public List<PlaylistDto> findAll(String username) {
        return musicMapper.findAll(username);
    }

    @Override
    public void playlistUpdate(String playlistId, String playlistName, String chzzkChannelId, String username) {
        musicMapper.playlistUpdate(playlistId, playlistName, chzzkChannelId, username);
    }

    @Override
    public void playlistDelete(String playlistId, String playlistName, String username) {
        musicMapper.playlistDelete(playlistId, playlistName, username);
    }
}
