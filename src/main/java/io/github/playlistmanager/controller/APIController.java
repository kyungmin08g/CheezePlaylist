package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.*;
import io.github.playlistmanager.service.impl.MusicServiceImpl;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class APIController {

    private final MusicServiceImpl musicService;
    private final UserServiceImpl userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public APIController(MusicServiceImpl musicService, UserServiceImpl userService, BCryptPasswordEncoder passwordEncoder) {
        this.musicService = musicService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/identical")
    public ResponseEntity<?> identicalUsername(@RequestParam String username) {
        String identicalUsername = userService.identicalUsername(username);

        if (identicalUsername == null) {
            return ResponseEntity.ok().body("undefined");
        }

        return ResponseEntity.status(201).body(identicalUsername);
    }

    @GetMapping("/find")
    public ResponseEntity<?> findUsername(@RequestParam String username) {
        String findPassword = Optional.ofNullable(userService.findByUsername(username)).map(JoinMemberDto::getPassword).orElse(null);

        if (findPassword == null) {
            return ResponseEntity.status(201).body("회원가입이 되어있지 않습니다.");
        }

        return ResponseEntity.ok().body(findPassword);
    }

    @GetMapping("/find/userid")
    public ResponseEntity<?> findEmail(@RequestParam String email) {
        String findUserId = Optional.ofNullable(userService.findByEmail(email)).map(JoinMemberDto::getUsername).orElse(null);

        if (findUserId == null) {
            return ResponseEntity.status(201).body("회원가입이 되어있지 않습니다.");
        }

        return ResponseEntity.ok().body(findUserId);
    }

    @GetMapping("/channel")
    public ResponseEntity<?> findChannel(@RequestParam String chzzkChannelId, SecurityContext context) {
        String username = (String) context.getAuthentication().getPrincipal();
        String channelId = musicService.getChannelName(chzzkChannelId);
        List<PlaylistDto> dtoList = musicService.findAll(username);

        if (!channelId.equals("(알 수 없음)")) {
            for (PlaylistDto dto : dtoList) {
                if (Objects.equals(dto.getChzzkChannelId(), chzzkChannelId)) {
                    return ResponseEntity.ok().body(dto.getChzzkChannelId());
                }
            }
        }

        return ResponseEntity.status(401).body("null");
    }

    @GetMapping("/playlist")
    public ResponseEntity<?> chzzk(@RequestParam String playlistName, @RequestParam String chzzkChannelId, @RequestParam String donationPrice, SecurityContext securityContext) {
        String uuid = UUID.randomUUID().toString();
        String username = (String) securityContext.getAuthentication().getPrincipal();

        if (!username.equals("anonymousUser")) {
            PlaylistDto dto = new PlaylistDto(username, uuid, playlistName, chzzkChannelId, donationPrice);
            musicService.saveChannelId(dto);
            return ResponseEntity.status(200).build();
        }

        return ResponseEntity.status(401).build();
    }

    @GetMapping("/playlists")
    public ResponseEntity<List<Map<String, String>>> getPlaylists(SecurityContext securityContext) {
        String username = (String) securityContext.getAuthentication().getPrincipal();

        List<Map<String, String>> playlists = new ArrayList<>();
        List<PlaylistDto> dto = musicService.findAll(username);

        for (PlaylistDto playlistDto : dto) {
            Map<String, String> playlist = new HashMap<>();
            playlist.put("playlistId", playlistDto.getPlaylistId());
            playlist.put("playlistName", playlistDto.getPlaylistName());
            playlist.put("chzzkChannelId", playlistDto.getChzzkChannelId());
            playlist.put("donationPrice", playlistDto.getDonationPrice());
            playlists.add(playlist);
        }

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(playlists);
    }

    @GetMapping("/list/{playlistId}")
    public ResponseEntity<List<Map<String, String>>> list(@PathVariable("playlistId") String playlistId) {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDto> listData = musicService.findById(playlistId);

        for (MusicFileDto dto : listData) {
            Map<String, String> musicData = new HashMap<>();

            String customTitle = dto.getTitle().replace("_", " ");
            String customRoomId = String.valueOf(dto.getRoomId());
            musicData.put("artist", dto.getArtist());
            musicData.put("roomId", customRoomId);
            musicData.put("title", customTitle);
            musicData.put("musicFileBytes", dto.getMusicFileBytes());
            musicData.put("date", dto.getDate());
            musicDatas.add(musicData);
        }

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(musicDatas);
    }

    @GetMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String playlistId, @RequestParam String artist, @RequestParam String title) {
        String customTitle = title.replace(" ", "_");
        musicService.delete(playlistId, artist, customTitle);
        log.info("해당 음악이 제거되었습니다.");

        return ResponseEntity.status(200).build();
    }

    @GetMapping("/playlist/update")
    public ResponseEntity<?> update(@RequestParam String playlistId, @RequestParam String playlistName, @RequestParam String chzzkChannelId, @RequestParam String donationPrice, SecurityContext securityContext) {
        String username = (String) securityContext.getAuthentication().getPrincipal();

        if (!username.equals("anonymousUser")) {
            musicService.playlistUpdate(playlistId, playlistName, chzzkChannelId, username, donationPrice);
            log.info("업데이트 되었습니다.");
            return ResponseEntity.status(200).build();
        }

        return ResponseEntity.status(401).build();
    }

    @GetMapping("/playlist/delete")
    public ResponseEntity<?> playlistDelete(@RequestParam String playlistId, @RequestParam String playlistName, SecurityContext securityContext) {
        String username = (String) securityContext.getAuthentication().getPrincipal();

        if (!username.equals("anonymousUser")) {
            musicService.playlistDelete(playlistId, playlistName, username);
            musicService.deleteById(playlistId);
            log.info("{}이 제거되었습니다.", playlistName);
            return ResponseEntity.status(200).build();
        }

        return ResponseEntity.status(401).body("anonymousUser");
    }

    @MessageMapping("/api/v1/message/{playlistId}")
    public ResponseEntity<?> message(@DestinationVariable("playlistId") String playlistId, MessageRequestDto messageRequestDTO) {
        musicService.memberMusicDownload(playlistId, messageRequestDTO.getArtist(), messageRequestDTO.getTitle());
        return ResponseEntity.status(200).build();
    }

    @MessageMapping("/api/v1/chat/{playlistId}")
    public ResponseEntity<?> chzzkChatMessage(@DestinationVariable("playlistId") String playlistId, String chatJson) {
        musicService.donationChat(playlistId, chatJson);
        return ResponseEntity.status(200).build();
    }
}
