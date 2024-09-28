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

    @DeleteMapping("/delete/{roomId}/{title}")
    public ResponseEntity<?> delete(@PathVariable("roomId") int roomId, @PathVariable("title") String title) {
        String customTitle = title.replace(" ", "_");

        service.deleteMusicFile(roomId, customTitle);
        System.out.println("roomId: " + roomId + ", title: " + title);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.ok().build();
    }

    @GetMapping("/list/{id}")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable("id") int id) {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDTO> listData = service.selectMusicFiles(id);

        for (MusicFileDTO dto : listData) {
            Map<String, String> musicData = new HashMap<>();

            String customTitle = dto.getTitle().replace("_", " ");
            String roomId = String.valueOf(dto.getRoomId());
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
        MusicFileDTO dto = service.findByTitle(roomId, customTitle);
        String changeTitle = dto.getTitle().replace("_", " ");

        MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                .roomId(roomId)
                .title(changeTitle)
                .musicFileBytes(dto.getMusicFileBytes()).build();

        messagingTemplate.convertAndSend("/sub/message/" + roomId, musicFileDTO);
    }

    // 방 생성
    @GetMapping("/create")
    public ResponseEntity<?> createRoom(@RequestParam String roomId, @RequestParam String roomName) {
        int id = Integer.parseInt(roomId);

        RoomDTO room = service.selectRoomById(id);
        if (room != null) {
            log.error("해당 아이디가 이미 있습니다.");
            return ResponseEntity.status(404).build();
        }

        RoomDTO roomDTO = RoomDTO.builder()
                .roomId(id)
                .roomName(roomName)
                .build();

        service.roomSave(roomDTO);

        return ResponseEntity.status(201).body("방 생성 성공!");
    }

    // 방 조회
    @GetMapping("/room/list")
    public ResponseEntity<List<RoomDTO>> roomList() {
        List<RoomDTO> allRoom = service.selectAllRooms();

        return ResponseEntity.ok().body(allRoom);
    }

    // 방 아이디를 통한 조회
    @GetMapping("/room/{roomId}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable("roomId") int roomId) {
        RoomDTO room = service.selectRoomById(roomId);
        if (room == null) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.status(200).body(room);
    }

    // 플레이리스트 생성
    @GetMapping("/playlist/create/{roomId}")
    public ResponseEntity<?> createPlaylist(@PathVariable("roomId") String roomId, @RequestParam String playlistName) {
        int id = Integer.parseInt(roomId);
        System.out.println("roomId: " + id + ", playlistName: " + playlistName);

        PlaylistDTO playlistDTO = PlaylistDTO.builder()
                .roomId(id)
                .playlistName(playlistName)
                .build();

        service.playlistSave(playlistDTO);

        return ResponseEntity.status(201).body("해당 방에 플레이리스트 생성 성공!");
    }

    // 모든 플레이리스트 조회
    @GetMapping("/playlist/list")
    public ResponseEntity<?> getPlaylist() {
        // TODO 나중에 구현해야함
        return ResponseEntity.status(200).body("해당 요청을 처리했습니다.");
    }

    @GetMapping("/")
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
                System.out.println("[채팅] " + message.getNickName() + ": " + message.getContent() + " [" + message.getSubscriptionMonth() + "개월 구독 중]");
            }

            @Override
            public void onDonation(DonationMessage message) {
                if (message.getSubscriptionMonth() == null) {
                    System.out.println("\u001B[33m[후원] " + message.getNickName() + ": " + message.getContent() + " [" + message.getPayAmount() + "원]\u001B[0m");
                    return;
                }

                System.out.println("\u001B[33m[후원] " + message.getNickName() + ": " + message.getContent() + " [" + message.getPayAmount() + "원] [" + message.getSubscriptionMonth() + "개월 구독 중]\u001B[0m");
            }
        });

        return ResponseEntity.status(200).build();
    }
}
