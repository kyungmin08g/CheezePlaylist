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

    @GetMapping("/playlist")
    public ResponseEntity<?> chzzk(@RequestParam String playlistName, @RequestParam String chzzkChannelId) {
        String uuid = UUID.randomUUID().toString();
        PlaylistDto dto = new PlaylistDto(uuid, playlistName, chzzkChannelId);
        musicService.saveChannelId(dto);

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/playlists")
    public ResponseEntity<List<Map<String, String>>> getPlaylists() {
        List<Map<String, String>> playlists = new ArrayList<>();
        List<PlaylistDto> dto = musicService.findAll();

        for (PlaylistDto playlistDto : dto) {
            Map<String, String> playlist = new HashMap<>();
            playlist.put("playlistId", playlistDto.getPlaylistId());
            playlist.put("playlistName", playlistDto.getPlaylistName());
            playlist.put("chzzkChannelId", playlistDto.getChzzkChannelId());
            playlists.add(playlist);
        }

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(playlists);
    }

    @GetMapping("/list/{playlistId}")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable("playlistId") String playlistId) {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDTO> listData = musicService.findById(playlistId);

        for (MusicFileDTO dto : listData) {
            Map<String, String> musicData = new HashMap<>();

            String customTitle = dto.getTitle().replace("_", " ");
            String customRoomId = String.valueOf(dto.getRoomId());
            musicData.put("artist", dto.getArtist());
            musicData.put("roomId", customRoomId);
            musicData.put("title", customTitle);
            musicData.put("musicFileBytes", dto.getMusicFileBytes());
            musicDatas.add(musicData);
        }

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(musicDatas);
    }

    @GetMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String playlistId, @RequestParam String artist, @RequestParam String title) {
        String customTitle = title.replace(" ", "_");
        musicService.delete(playlistId, artist, customTitle);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.ok().build();
    }

    @MessageMapping("/api/v1/message/{playlistId}")
    public ResponseEntity<?> message(@DestinationVariable("playlistId") String playlistId, MessageRequestDTO messageRequestDTO) {
        musicService.memberMusicDownload(playlistId, messageRequestDTO.getArtist(), messageRequestDTO.getTitle());
        return ResponseEntity.status(200).build();
    }

    @MessageMapping("/api/v1/chat/{playlistId}")
    public ResponseEntity<?> chzzkChatMessage(@DestinationVariable("playlistId") String playlistId, String chatJson) {
        musicService.donationChat(playlistId, chatJson);
        return ResponseEntity.status(200).build();
    }
}
