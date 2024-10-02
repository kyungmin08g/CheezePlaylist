package io.github.playlistmanager.controller;

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
@RequestMapping("/api/v1")
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
    public ResponseEntity<?> signUp(@RequestParam("username") String username, @RequestParam("email") String email, @RequestParam("password") String password) {
        JoinMemberDTO memberDto = JoinMemberDTO.builder()
                .username(username)
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .role("ROLE_USER").build();

        userService.save(memberDto);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/{playlistId}/{name}/{chzzkChannelId}/on")
    public ResponseEntity<?> chzzk(@PathVariable("playlistId") String playlistId, @PathVariable("name") String name, @PathVariable("chzzkChannelId") String chzzkChannelId) {
        PlaylistDto dto = new PlaylistDto(playlistId, name, chzzkChannelId);
        musicService.saveChannelId(dto);

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/list/{playlistId}")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable("playlistId") int playlistId) {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDTO> listData = musicService.findById(playlistId);

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

    @DeleteMapping("/delete/{playlistId}/{artist}/{title}")
    public ResponseEntity<?> delete(@PathVariable("playlistId") int playlistId, @PathVariable("artist") String artist, @PathVariable("title") String title) {
        String customTitle = title.replace(" ", "_");
        musicService.delete(playlistId, artist, customTitle);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.ok().build();
    }

    @MessageMapping("/api/v1/message/{playlistId}")
    public ResponseEntity<?> message(@DestinationVariable("playlistId") int playlistId, MessageRequestDTO messageRequestDTO) {
        musicService.memberMusicDownload(playlistId, messageRequestDTO.getArtist(), messageRequestDTO.getTitle());
        return ResponseEntity.status(200).build();
    }

    @MessageMapping("/api/v1/chat/{playlistId}")
    public ResponseEntity<?> chzzkChatMessage(@DestinationVariable("playlistId") int playlistId, String chatJson) {
        musicService.donationChat(playlistId, chatJson);
        return ResponseEntity.status(200).build();
    }
}
