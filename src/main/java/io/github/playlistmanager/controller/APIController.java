package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MessageRequestDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

//    @GetMapping("/mp3")
//    public ResponseEntity<String> index(@RequestParam String artist, @RequestParam String title) {
//        service.musicDownload(artist, title);
//
//        return ResponseEntity.ok().body("다운로드 됨");
//    }

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

}
