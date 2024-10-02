package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.ChzzkChannelConnectDto;
import io.github.playlistmanager.service.impl.MusicServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    private final MusicServiceImpl musicService;

    public ViewController(
            MusicServiceImpl musicService
    ) {
        this.musicService = musicService;
    }

    @GetMapping("/")
    public String index() {
        return "Test2";
    }

    @GetMapping("/{id}/{name}/on")
    public String view(@PathVariable("id") String id, @PathVariable("name") String name, Model model) {
        ChzzkChannelConnectDto connectDto = musicService.chzzkChannelConnect(musicService.findByIdAndPlaylistName(id, name));

        model.addAttribute("playlistId", connectDto.getPlaylistId());
        model.addAttribute("chatChannelId", connectDto.getChatChannelId());
        model.addAttribute("accessToken", connectDto.getAccessToken());
        model.addAttribute("serverId", connectDto.getServerId());

        return "Test";
    }

}
