package io.github.playlistmanager.controller;

import io.github.chzzkapi.api.ChzzkAPI;
import io.github.chzzkapi.listener.ChatListener;
import io.github.chzzkapi.message.ChatMessage;
import io.github.chzzkapi.message.DonationMessage;
import io.github.playlistmanager.dto.*;
import io.github.playlistmanager.service.impl.MusicServiceImpl;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class APIController {

    private final UserServiceImpl userService;
    private final MusicServiceImpl musicService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public APIController(
            UserServiceImpl userService,
            MusicServiceImpl musicService,
            BCryptPasswordEncoder bCryptPasswordEncoder
    ) {
        this.userService = userService;
        this.musicService = musicService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @GetMapping("/signup")
    public String signUp(@RequestParam("username") String username, @RequestParam("email") String email, @RequestParam("password") String password) {
        JoinMemberDTO memberDto = JoinMemberDTO.builder()
                .username(username)
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .role("ROLE_USER").build();

        userService.save(memberDto);
        return "회원가입 성공!";
    }

    @GetMapping("/list/{id}")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable("id") int roomId) {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDTO> listData = musicService.findById(roomId);

        for (MusicFileDTO dto : listData) {
            Map<String, String> musicData = new HashMap<>();

            String customTitle = dto.getTitle().replace("_", " ");
            String customRoomId = String.valueOf(dto.getRoomId());
            musicData.put("artist", dto.getArtist());
            musicData.put("roomId", customRoomId);
            musicData.put("title", customTitle);
            musicData.put("musicFileBytes", Base64.getEncoder().encodeToString(dto.getMusicFileBytes())); // byte[]을 Base64로 인코딩
            musicDatas.add(musicData);
        }

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(musicDatas);
    }

    @DeleteMapping("/delete/{roomId}/{artist}/{title}")
    public ResponseEntity<?> delete(@PathVariable("roomId") int roomId, @PathVariable("artist") String artist, @PathVariable("title") String title) {
        String customTitle = title.replace(" ", "_");
        musicService.delete(roomId, artist, customTitle);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.ok().build();
    }

    @MessageMapping("/message/{roomId}")
    public void message(@DestinationVariable("roomId") int roomId, MessageRequestDTO messageRequestDTO) {
        musicService.memberMusicDownload(roomId, messageRequestDTO.getArtist(), messageRequestDTO.getTitle());
    }

    @MessageMapping("/chat/{roomId}")
    public void chzzkChatMessage(@DestinationVariable("roomId") int roomId, String chatJson) {
        musicService.donationChat(roomId, chatJson);
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
}
