package io.github.playlistmanager.controller;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class APIController {

    private final UserServiceImpl service;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final SimpMessagingTemplate messagingTemplate;

    public APIController(UserServiceImpl service, BCryptPasswordEncoder bCryptPasswordEncoder, SimpMessagingTemplate messagingTemplate) {
        this.service = service;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.messagingTemplate = messagingTemplate;
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

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(musicDatas);
    }

    @MessageMapping("/message/{roomId}")
    public void message(@DestinationVariable("roomId") int roomId, MessageRequestDTO messageRequestDTO) {
        service.musicDownload(roomId, messageRequestDTO.getArtist(), messageRequestDTO.getTitle());

        String customTitle = messageRequestDTO.getTitle().replace(" ", "_");
        MusicFileDTO dto = service.findByMusic(roomId, messageRequestDTO.getArtist(), customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                .artist(messageRequestDTO.getArtist())
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        messagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }

    @DeleteMapping("/delete/{roomId}/{artist}/{title}")
    public ResponseEntity<?> delete(@PathVariable("roomId") int roomId, @PathVariable("artist") String artist, @PathVariable("title") String title) {
        String customTitle = title.replace(" ", "_");

        System.out.println(artist);
        service.deleteMusicFile(roomId, artist, customTitle);
        System.out.println("roomId: " + roomId + ", artist: " + artist + ", title: " + title);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.ok().build();
    }

    @GetMapping("/chzzk")
    public ResponseEntity<?> getChzzk(@RequestParam String channelId) {
        String channelName = ChzzkAPI.getChannelName(channelId);
        System.out.println(ChzzkAPI.getChannelDescription(channelId));
        System.out.println(ChzzkAPI.getFollowCount(channelId));
        System.out.println(ChzzkAPI.getChannelChatRule(channelId));
        System.out.println("방송 여부: " + ChzzkAPI.isLive(channelId));

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

        return ResponseEntity.status(200).build();
    }
}
