package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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

//    @GetMapping("/")
//    @ResponseBody
//    public ResponseEntity<String> sdaf(@RequestParam String artist, @RequestParam String title) {
//        String music = artist + title;
//        String s = service.searchVideo(music);
//
//        return ResponseEntity.ok().body(s);
//    }

    @GetMapping("/mp3")
    @ResponseBody
    public ResponseEntity<String> index(@RequestParam String artist, @RequestParam String title) {
        service.musicDownload(artist, title);

        return ResponseEntity.ok().body("됨");
    }

    @GetMapping("/")
    public String index() {
        return "Test";
    }

    @GetMapping("/play")
    public ResponseEntity<byte[]> playMusic() {
        MusicFileDTO musicBytes = service.findByTitle("MIA.mp3");
        byte[] byteData = musicBytes.getData();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"MIA.mp3\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteData);
    }

}
