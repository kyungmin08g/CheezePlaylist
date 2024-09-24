package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.*;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.r2turntrue.chzzk4j.Chzzk;
import xyz.r2turntrue.chzzk4j.ChzzkBuilder;
import xyz.r2turntrue.chzzk4j.chat.*;
import xyz.r2turntrue.chzzk4j.types.channel.ChzzkChannel;
import xyz.r2turntrue.chzzk4j.types.channel.ChzzkChannelRules;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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


        return ResponseEntity.status(200).body("해당 요청을 처리했습니다.");
    }

    // 치지직
    @GetMapping("/")
    public ResponseEntity<?> Chzzk(@RequestParam String channelId) throws IOException, InterruptedException {
        Chzzk chzzk = new ChzzkBuilder().build();

        ChzzkChannel channel = chzzk.getChannel(channelId);
        System.out.println(channel.getChannelName());

        ChzzkChat chat = chzzk.chat(channelId).withChatListener(new ChatEventListener() {
            @Override
            public void onConnect(ChzzkChat chat, boolean isReconnecting) {
                System.out.println("채팅창과 연결됨.");
            }

            @Override
            public void onConnectionClosed(int code, String reason, boolean remote, boolean tryingToReconnect) {

            }

            @Override
            public void onError(Exception ex) {

            }

            @Override
            public void onChat(ChatMessage msg) {
//                System.out.println(msg);
//
                if (msg.getProfile() == null) {
                    System.out.println("[Chat] 익명: " + msg.getContent());
                    return;
                }

                if (msg.getContent().matches("^[^ -]+ - [^ -]+$")) {
                    System.out.println("노래 형식입니다.");
                }
//
                System.out.println("[Chat] " + msg.getProfile().getNickname() + ": " + msg.getContent());
            }

            @Override
            public void onDonationChat(DonationMessage msg) {
                if (msg.getProfile() == null) {

                    System.out.println("[Donation] 익명: " + msg.getContent() + " [" + msg.getPayAmount() + "원]");
                    return;
                }

                System.out.println("[Donation] " + msg.getProfile().getNickname() + ": " + msg.getContent() + " [" + msg.getPayAmount() + "원]");
            }

            @Override
            public void onSubscriptionChat(SubscriptionMessage msg) {

            }
        }).build();

        chat.connectBlocking();
//        Thread.sleep(100);
//        chat.closeBlocking();

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/web")
    public ResponseEntity<?> web() {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com")
                .build().get().uri("service/v1/channels/a7e175625fdea5a7d98428302b7aa57f").retrieve().bodyToMono(String.class);

        System.out.println(response.block());

        return ResponseEntity.status(200).build();
    }

}
