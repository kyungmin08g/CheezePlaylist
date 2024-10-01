package io.github.playlistmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chzzkapi.api.ChzzkAPI;
import io.github.chzzkapi.listener.ChatListener;
import io.github.chzzkapi.message.ChatMessage;
import io.github.chzzkapi.message.DonationMessage;
import io.github.playlistmanager.dto.*;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
public class APIController {

    private final UserServiceImpl service;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private int count = 0;

    public APIController(UserServiceImpl service, BCryptPasswordEncoder bCryptPasswordEncoder, SimpMessagingTemplate messagingTemplate) {
        this.service = service;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/signup")
    public String signUp(@RequestParam("username") String username, @RequestParam("email") String email, @RequestParam("password") String password) {
        JoinMemberDTO memberDTO = JoinMemberDTO.builder()
                .username(username)
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .role("ROLE_USER").build();

        service.signUp(memberDTO);

        return "회원가입 성공!";
    }

    @GetMapping("/list/{id}")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable("id") int id) {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDTO> listData = service.selectMusicFiles(id);
//        for (MusicFileDTO musicFileDTO : listData) {
//            System.out.println(musicFileDTO.getTitle());
//        }

        for (MusicFileDTO dto : listData) {
            Map<String, String> musicData = new HashMap<>();

            String customTitle = dto.getTitle().replace("_", " ");
            String roomId = String.valueOf(dto.getRoomId());
            musicData.put("artist", dto.getArtist());
            musicData.put("roomId", roomId);
            musicData.put("title", customTitle);
            musicData.put("musicFileBytes", Base64.getEncoder().encodeToString(dto.getMusicFileBytes())); // byte[]을 Base64로 인코딩
            musicDatas.add(musicData);
        }

        for (Map<String, String> dto : musicDatas) {
            System.out.println(dto.get("title"));
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(musicDatas);
    }

    @MessageMapping("/message/{roomId}")
    public void message(@DestinationVariable("roomId") int roomId, MessageRequestDTO messageRequestDTO) {
        musicDownloadAndSave(roomId, messageRequestDTO.getArtist(), messageRequestDTO.getTitle());
    }

    @MessageMapping("/chat/{roomId}")
    public void chzzkChatMessage(@DestinationVariable("roomId") int roomId, String chatData) {
        donationChat(roomId, chatData);
    }

    private void don(int roomId, String title) {
        service.musicDownload(roomId, "GEMINI", title);

        String customTitle = title.replace(" ", "_");
        MusicFileDTO dto = service.findByMusic(roomId, "GEMINI", customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                .artist("GEMINI")
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        messagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }

    private void donationChat(int roomId, String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            JsonNode bdyJson = jsonNode.get("bdy");
            if (bdyJson == null) return;

            JsonNode auth = bdyJson.get("auth"); // 처음에 연결할떄 생기는 JSON
            if (auth != null) return;

            for (JsonNode bdyNode : bdyJson) {
                JsonNode msgTypeCode = bdyNode.get("msgTypeCode");
                if (msgTypeCode == null) return;

                if (msgTypeCode.asInt() == 10) { // 채팅 타입이면..
                    String profile = bdyNode.get("profile").asText();
                    JsonNode message = bdyNode.get("msg");

                    if (profile.equals("null")) { // 프로필이 없을 떄
                        System.out.println("익명: " + message.asText());

                        if (count == 0) don(roomId, "MIA");
                        if (count == 1) don(roomId, "UFO");
                        if (count == 2) don(roomId, "Slo-mo");
                        if (count == 3) don(roomId, "Sleep");
                        if (count == 4) don(roomId, "Going");
                        if (count == 5) don(roomId, "Hola");
                        count++;
                    } else {
                        JsonNode profileNode = objectMapper.readTree(profile);
                        JsonNode nickname = profileNode.get("nickname");
                        JsonNode streamingProperty = profileNode.get("streamingProperty"); // 구독

                        if (monthsCalculation(streamingProperty) != null) {
                            System.out.println(nickname.asText() + ": " + message.asText() + " [" + monthsCalculation(streamingProperty) + " 구독 중]");
                            if (count == 0) don(roomId, "MIA");
                            if (count == 1) don(roomId, "UFO");
                            if (count == 2) don(roomId, "Slo-mo");
                            if (count == 3) don(roomId, "Sleep");
                            if (count == 4) don(roomId, "Going");
                            if (count == 5) don(roomId, "Hola");
                        } else {
                            System.out.println(nickname.asText() + ": " + message.asText());
                            if (count == 0) don(roomId, "MIA");
                            if (count == 1) don(roomId, "UFO");
                            if (count == 2) don(roomId, "Slo-mo");
                            if (count == 3) don(roomId, "Sleep");
                            if (count == 4) don(roomId, "Going");
                            if (count == 5) don(roomId, "Hola");
                        }

                    }

                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    // 구독 년 및 개월 구하는 메소드
    private static String monthsCalculation(JsonNode streamingPropertyNode) {
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

    @DeleteMapping("/delete/{roomId}/{artist}/{title}")
    public ResponseEntity<?> delete(@PathVariable("roomId") int roomId, @PathVariable("artist") String artist, @PathVariable("title") String title) {
        String customTitle = title.replace(" ", "_");
        service.deleteMusicFile(roomId, artist, customTitle);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.ok().build();
    }

    @GetMapping("/chzzk")
    public ResponseEntity<?> getChzzk(@RequestParam String channelId) {
        String channelName = ChzzkAPI.getChannelName(channelId);

        if (ChzzkAPI.isLive(channelId)) {
            ChzzkAPI.chat(channelId, new ChatListener() {
                @Override
                public void onConnect() {
                    System.out.println(channelName + "님 채널에 연결되었습니다.");
                }

                @Override
                public void onChat(ChatMessage message) {
                    if (message.getSubscriptionMonth() == null) {
                        System.out.println("[채팅] " + message.getNickName() + ": " + message.getContent());
                        return;
                    }
                    System.out.println("[채팅] " + message.getNickName() + ": " + message.getContent() + " [" + message.getSubscriptionMonth() + " 구독 중]");
                }

                @Override
                public void onDonation(DonationMessage message) {
                    if (message.getSubscriptionMonth() == null) {
                        System.out.println("\u001B[33m[후원] " + message.getNickName() + ": " + message.getContent() + " [" + message.getPayAmount() + "원]\u001B[0m");
                        return;
                    }
                    System.out.println("\u001B[33m[후원] " + message.getNickName() + ": " + message.getContent() + " [" + message.getPayAmount() + "원] [" + message.getSubscriptionMonth() + " 구독 중]\u001B[0m");
                }

                @Override
                public void onDisconnect(boolean open) {
                    if (!open) System.out.println("연결이 닫혔습니다.");
                }
            });
        }

        return ResponseEntity.status(200).build();
    }

    private void musicDownloadAndSave(int roomId, String artist, String title) {
        service.musicDownload(roomId, artist, title);

        String customTitle = title.replace(" ", "_");
        MusicFileDTO dto = service.findByMusic(roomId, "GEMINI", customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                .artist(artist)
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        messagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }
}
