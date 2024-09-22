package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class TestController {

    private final UserServiceImpl service;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public TestController(BCryptPasswordEncoder bCryptPasswordEncoder, UserServiceImpl service) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.service = service;
    }

    @GetMapping("/signup")
    @ResponseBody
    public String signUp(@RequestParam("username") String username, @RequestParam("email") String email, @RequestParam("password") String password) {
        JoinMemberDTO memberDTO = JoinMemberDTO.builder()
                .username(username)
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .role("ROLE_USER").build();

        service.signUp(memberDTO);

        return "회원가입 성공!";
    }

    @GetMapping("/mp3")
    @ResponseBody
    public ResponseEntity<String> index(@RequestParam String artist, @RequestParam String title) {
        service.musicDownload(artist, title);

        return ResponseEntity.ok().body("다운로드 됨");
    }

    @GetMapping("/{id}")
    public String index(@PathVariable("id") String id, Model model) {
        model.addAttribute("id", id);
        return "Test";
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> list() {
        List<Map<String, String>> musicDatas = new ArrayList<>();
        List<MusicFileDTO> listData = service.selectMusicFiles();

        for (MusicFileDTO dto : listData) {
            Map<String, String> musicData = new HashMap<>();

            String customTitle = dto.getName().replace("_", " ");
            musicData.put("name", customTitle);
            musicData.put("data", Base64.getEncoder().encodeToString(dto.getData())); // byte[]을 Base64로 인코딩
            musicDatas.add(musicData);
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(musicDatas);
    }

}
