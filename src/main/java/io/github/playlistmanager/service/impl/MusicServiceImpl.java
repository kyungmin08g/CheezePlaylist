package io.github.playlistmanager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.playlistmanager.dto.ChzzkChannelConnectDto;
import io.github.playlistmanager.dto.MusicFileDto;
import io.github.playlistmanager.dto.PlaylistDto;
import io.github.playlistmanager.mapper.MusicMapper;
import io.github.playlistmanager.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MusicServiceImpl implements MusicService {

    @Value("${spotify.clientId}")
    String clientId;
    @Value("${spotify.clientSecret}")
    String clientSecret;

    private final String key;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MusicMapper musicMapper;
    private final ObjectMapper objectMapper;
    int count = 0;

    private String chatChannelId = null;
    private String accessToken = null;

    public MusicServiceImpl(
            @Value("${youtube.api-key}") String key,
            SimpMessagingTemplate simpMessagingTemplate,
            MusicMapper musicMapper
    ) {
        this.key = key;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.musicMapper = musicMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void memberMusicDownload(String roomId, String artist, String title) {
        musicDownload(roomId, artist, title, null, null, null);

        String customTitle = title.replace(" ", "_");
        MusicFileDto dto = musicMapper.findByMusic(roomId, artist, customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDto musicFileDTO = MusicFileDto.builder()
                .artist(artist)
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        simpMessagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }

    @Override
    public void donationMusicDownloader(String roomId, String artist, String title, String donationUsername, String donationPrice, String donationSubscriber) {
        musicDownload(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);

        String customTitle = title.replace(" ", "_");
        MusicFileDto dto = musicMapper.findByMusic(roomId, artist, customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDto musicFileDTO = MusicFileDto.builder()
                .artist(artist)
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        simpMessagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }

    @Override
    public void donationMusicDownload(String roomId, String donationContent, String donationUsername, String donationPrice, String donationSubscriber) {
        String artist;
        String title;

        // 테스트 용
        donationMusicDownloader(roomId, "Gemini", "MIA", donationUsername, donationPrice, donationSubscriber);

        if(donationContent.matches("^(.*) - (.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 1);
            title = donationContent.substring(donationContent.indexOf("-") + 2);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
        } else if (donationContent.matches("^(.*) -(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 1);
            title = donationContent.substring(donationContent.indexOf("-") + 1);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
        } else if (donationContent.matches("^(.*)- (.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-"));
            title = donationContent.substring(donationContent.indexOf("-") + 2);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
        } else if (donationContent.matches("^(.*)-(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-"));
            title = donationContent.substring(donationContent.indexOf("-") + 1);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
        } else if (donationContent.matches("^(.*) - {2}(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 1);
            title = donationContent.substring(donationContent.indexOf("-") + 3);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
        } else if (donationContent.matches("^(.*) {2}- (.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 2);
            title = donationContent.substring(donationContent.indexOf("-") + 2);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
        } else if (donationContent.matches("^(.*) {2}- {2}(.*)$")) {
            artist = donationContent.substring(0, donationContent.indexOf("-") - 2);
            title = donationContent.substring(donationContent.indexOf("-") + 3);
            donationMusicDownloader(roomId, artist, title, donationUsername, donationPrice, donationSubscriber);
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
        PlaylistDto donationPrice = musicMapper.findByPlaylistId(roomId);

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

                    if (donationPrice.getDonationPrice().equals(payAmount)) {
                        if (profile.equals("null")) { // 프로필이 없을 떄
                            System.out.println("\u001B[33m[후원] 익명: " + message.asText() + " [" + payAmount + "원]\u001B[0m");
                            donationMusicDownload(roomId, message.asText(), "익명", payAmount, null);
                        } else {
                            JsonNode profileNode = objectMapper.readTree(profile);
                            JsonNode nickname = profileNode.get("nickname");
                            JsonNode streamingProperty = profileNode.get("streamingProperty"); // 구독

                            String subscriptionMonth = calculateMonthsSubscribed(streamingProperty);
                            if (subscriptionMonth != null) {
                                System.out.println("\u001B[33m[후원] " + nickname.asText() + ": " + message.asText() + " [" + payAmount + "원]" + " [" + subscriptionMonth + " 구독 중]" + "\u001B[0m");
                                donationMusicDownload(roomId, message.asText(), nickname.asText(), payAmount, subscriptionMonth);
                            } else {
                                System.out.println("\u001B[33m[후원] " + nickname.asText() + ": " + message.asText() + " [" + payAmount + "원]" + "\u001B[0m");
                                donationMusicDownload(roomId, message.asText(), nickname.asText(), payAmount, null);
                            }
                        }
                    }

                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    // 쿼리를 받으면 동영상을 검색하는 메소드임
    @Override
    public String searchVideo(String query) {
        String searchUrl = "https://www.youtube.com/results?search_query=" + query + " lyrics&sp=EgIYBA==";
        try {
            Document document = Jsoup.connect(searchUrl).get();
            Elements scriptElements = document.select("script");

            for (Element scriptElement : scriptElements) {
                String scriptContent = scriptElement.html();

                String ytInitialDataPrefix = "var ytInitialData = ";
                int startIndex = scriptContent.indexOf(ytInitialDataPrefix);

                if (startIndex != -1) {
                    startIndex += ytInitialDataPrefix.length();
                    int endIndex = scriptContent.indexOf("};", startIndex) + 1;
                    String ytInitialDataJson = scriptContent.substring(startIndex, endIndex);

                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(ytInitialDataJson);

                    JsonNode contents = jsonNode.path("contents")
                            .path("twoColumnSearchResultsRenderer")
                            .path("primaryContents")
                            .path("sectionListRenderer")
                            .path("contents");

                    for (JsonNode content : contents) {
                        JsonNode itemSection = content.path("itemSectionRenderer").path("contents");

                        for (JsonNode item : itemSection) {
                            JsonNode videoRenderer = item.path("videoRenderer");
                            String videoId = videoRenderer.path("videoId").asText();
                            if (!videoId.isEmpty()) return "https://www.youtube.com/watch?v=" + videoId;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void musicDownload(String roomId, String artist, String title, String donationUsername, String donationPrice, String donationSubscriber) {
        String query = artist + " - " + title; // 하나의 문자열로 만듬
        String customTitle = title.replace(" ", "_");

        // 검색 API 호출 전에 해당 음악이 있는지 체크
        String optionalMusicFileDTO = Optional.ofNullable(findByMusic(roomId, artist, customTitle)).map(MusicFileDto::getTitle).orElse("Untitled");
        if (!optionalMusicFileDTO.equals("Untitled")) {
            log.info("해당 음악이 이미 있음");
            return;
        }

        // 다운로드 휫수를 줄이기 위해 해당음악이 있으면 가져와 저장
        MusicFileDto optionalTitleDTO = findByArtistAndTitle(artist, customTitle);
        if (optionalTitleDTO != null) {
            MusicFileDto musicFileDTO = MusicFileDto.builder()
                    .artist(artist)
                    .roomId(roomId)
                    .title(customTitle)
                    .musicFileBytes(optionalTitleDTO.getMusicFileBytes())
                    .donationUsername(donationUsername)
                    .donationPrice(donationPrice)
                    .donationSubscriber(donationSubscriber)
                    .build();

            save(musicFileDTO);
            log.info("DB에 음악이 있으므로 검색하지 않고 저장함.");
            return;
        }

        try {
            String url = searchVideo(query); // 유튜브 영상의 주소를 만듬
            if (url == null) {
                log.info("해당 음악을 다운로드하고 DB에 저장하는 로직에 예외를 발생시켰습니다.");
                return;
            }
            conversionAndDownload(roomId, url, artist, customTitle, donationUsername, donationPrice, donationSubscriber); // URL과 해당 음악의 제목을 넣으면 음악 byte[]를 DB에 저장함

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

        // yt-dlp의 출력을 ffmpeg의 입력으로 연결함
        try (InputStream ytDlpOutput = ytDlpProcess.getInputStream();
             InputStream ffmpegInput = ffmpegProcess.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // yt-dlp의 출력을 ffmpeg에 연결함
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

            // ffmpeg의 출력을 byte array로 읽ㅇㅓ!!!
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = ffmpegInput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            log.info("ffmpeg 출력을 성공적으로 읽음");

            ytDlpToFfmpeg.join(); // yt-dlp 쓰레드가 끝날 때까지 기다림
            log.info("yt-dlp 스레드가 완료됨");

            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("오디오 처리 중 오류 발생: {}", e.getMessage());
            throw e; // 예외를 던져서 컨트롤러에서 처리할 수 있도록 함
        } finally {
            ytDlpProcess.waitFor();
            ffmpegProcess.waitFor();
            log.info("yt-dlp 및 ffmpeg 프로세스가 완료됨");
        }
    }

    @Override
    public void conversionAndDownload(String roomId, String youtubeURL, String artist, String customTitle, String donationUsername, String donationPrice, String donationSubscriber) throws IOException, InterruptedException {
        byte[] mp3Data = mp3Conversion(youtubeURL);
        String title = customTitle.replace("_", " ");
        byte[] imageData = spotifyMusicAlbum(artist, title);

        // DB에서 Title을 통해 조회하게 되는데 결과가 null인 경우에는 로그만 찍고 아래 로직을 실행하도록하는 검사 작업 -> 데이터가 존재하지 않으면 추가하는게 맞으니까
        if (mp3Data == null) {
            log.info("URL에 대한 데이터가 없음");
        } else {
            // MusicFileDTO 객체 생성
            MusicFileDto musicFileDTO;
            if (imageData != null) {
                musicFileDTO = MusicFileDto.builder()
                        .roomId(roomId)
                        .image(Base64.getEncoder().encodeToString(imageData))
                        .artist(artist)
                        .title(customTitle)
                        .musicFileBytes(Base64.getEncoder().encodeToString(mp3Data))
                        .donationUsername(donationUsername)
                        .donationPrice(donationPrice)
                        .donationSubscriber(donationSubscriber)
                        .build();

            } else {
                musicFileDTO = MusicFileDto.builder()
                        .roomId(roomId)
                        .image("null")
                        .artist(artist)
                        .title(customTitle)
                        .musicFileBytes(Base64.getEncoder().encodeToString(mp3Data))
                        .donationUsername(donationUsername)
                        .donationPrice(donationPrice)
                        .donationSubscriber(donationSubscriber)
                        .build();

            }
            save(musicFileDTO);
            log.info("DB에 저장 성공!");
        }
    }

    // Spotify API를 사용해서 음악 앨범 이미지 가져오기
    @Override
    public byte[] spotifyMusicAlbum(String artist, String title) {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        String responseToken = WebClient.builder().build().post()
                .uri("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic " + encodedCredentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode json = objectMapper.readTree(responseToken);
            String accessToken = json.get("access_token").asText();

            String response = WebClient.builder().baseUrl("https://api.spotify.com/").build()
                    .get()
                    .uri("v1/search?q=track:" + title + " artist:" + artist + "&type=track")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode musicJson = objectMapper.readTree(response);
            JsonNode tracks = musicJson.get("tracks");
            JsonNode items = tracks.get("items");
            for (JsonNode item : items) {
                if (count == 0) {
                    JsonNode album = item.get("album");
                    JsonNode images = album.get("images");

                    for (JsonNode image : images) {
                        if (count == 0) {
                            String url = image.get("url").asText();
                            System.out.println("해당 음악의 대한 앨범 URL: " + url);
                            count++;

                            HttpClient client = HttpClient.newHttpClient();
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .GET()
                                    .build();

                            try {
                                HttpResponse<InputStream> response1 = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                                try (InputStream in = response1.body();
                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    while ((bytesRead = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, bytesRead);
                                    }
                                    return out.toByteArray();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }
                }
            }
            count = 0;

        } catch (Exception e) {
            e.fillInStackTrace();
        }

        return null;
    }

//    @Override
    public ChzzkChannelConnectDto chzzkChannelConnect(PlaylistDto playlistDto) {
        String channelId = playlistDto.getChzzkChannelId();
//        String channelName = getChannelName(channelId);
//        String chatChannelId = getChatChannelId(channelId);
//        String accessToken = getAccessToken(chatChannelId);
//
//        int serverId = 0;
//        for (char i : chatChannelId.toCharArray()) {
//            serverId += Character.getNumericValue(i);
//        }
//        serverId = Math.abs(serverId) % 9 + 1;
//
//        log.info("\u001B[32m{}님 채널에 연결하는 로직이 정상적으로 처리되었습니다.\u001B[0m", channelName);
        return ChzzkChannelConnectDto.builder()
                .playlistId(playlistDto.getPlaylistId())
                .chatChannelId("")
                .accessToken("")
                .serverId("")
                .build();
    }

    @Override
    public void save(MusicFileDto musicFileDTO) {
        musicMapper.save(musicFileDTO);
    }

    @Override
    public MusicFileDto findByMusic(String roomId, String artist, String title) {
        return musicMapper.findByMusic(roomId, artist, title);
    }

    @Override
    public List<MusicFileDto> findById(String roomId) {
        return musicMapper.findById(roomId);
    }

    @Override
    public MusicFileDto findByArtistAndTitle(String artist, String title) {
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
    public PlaylistDto findByPlaylistId(String playlistId) {
        return musicMapper.findByPlaylistId(playlistId);
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
    public void playlistUpdate(String playlistId, String playlistName, String chzzkChannelId, String username, String donationPrice) {
        musicMapper.playlistUpdate(playlistId, playlistName, chzzkChannelId, username, donationPrice);
    }

    @Override
    public void playlistDelete(String playlistId, String playlistName, String username) {
        musicMapper.playlistDelete(playlistId, playlistName, username);
    }

    // 채널 이름 구하는 메소드
    public String getChannelName(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("channelName").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 설명 구하는 메소드
    private String getChannelDescription(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("channelDescription").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 팔로워 상태 구하는 메소드
    private String getFollowCount(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("followerCount").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 채팅 규칙 받는 메소드
    private String getChannelChatRule(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId + "/chat-rules")
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("rule").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 라이브 상태 구하는 메소드
    private boolean isLive(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("openLive").asBoolean();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return false;
    }

    // 채팅 아이디 받기 메소드
    private String getChatChannelId(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("/polling/v3/channels/" + channelId + "/live-status")
                .retrieve().bodyToMono(String.class);

        try {
            ObjectMapper liveStatusObjectMapper = new ObjectMapper();
            JsonNode liveStatusJson = liveStatusObjectMapper.readTree(response.block());
            JsonNode liveStatusContentJson = liveStatusJson.get("content");
            chatChannelId = liveStatusContentJson.get("chatChannelId").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return chatChannelId;
    }

    // 액세스 토큰 받기 메소드
    private String getAccessToken(String chatChannelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://comm-api.game.naver.com").build()
                .get()
                .uri("nng_main/v1/chats/access-token?channelId=" + chatChannelId + "&chatType=STREAMING")
                .retrieve().bodyToMono(String.class);

        try {
            ObjectMapper accessTokenObjectMapper = new ObjectMapper();
            JsonNode accessTokenJson = accessTokenObjectMapper.readTree(response.block());
            JsonNode accessTokenContentJson = accessTokenJson.get("content");
            accessToken = accessTokenContentJson.get("accessToken").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return accessToken;
    }
}
