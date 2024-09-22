package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.MessageRequestDTO;
import io.github.playlistmanager.dto.MessageResponseDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;

@Controller
@Slf4j
public class MessageController {

    private SimpMessagingTemplate messagingTemplate;
    private UserServiceImpl service;

    public MessageController(SimpMessagingTemplate messagingTemplate, UserServiceImpl service) {
        this.messagingTemplate = messagingTemplate;
        this.service = service;
    }

    @GetMapping("/me")
    public String me() {
        return "STOMP";
    }

    @MessageMapping("/message/{id}")
    public void message(@DestinationVariable("id") String id, MessageRequestDTO messageRequestDTO) {
        log.info("MessageController: {}",  "들어옴");

        service.musicDownload(messageRequestDTO.getArtist(), messageRequestDTO.getTitle());

        String customTitle = messageRequestDTO.getTitle().replace(" ", "_");
        MusicFileDTO dto = service.findByTitle(customTitle);
        String changeTitle = dto.getName().replace("_", " ");

        MusicFileDTO musicFileDTO = MusicFileDTO.builder()
                        .name(changeTitle)
                        .data(dto.getData()).build();

        messagingTemplate.convertAndSend("/sub/message/" + id, musicFileDTO);
    }

}
