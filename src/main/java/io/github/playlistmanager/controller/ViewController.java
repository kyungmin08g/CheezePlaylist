package io.github.playlistmanager.controller;

import io.github.playlistmanager.dto.ChzzkChannelConnectDto;
import io.github.playlistmanager.service.impl.MusicServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ViewController {
    private final MusicServiceImpl musicService;

    public ViewController(MusicServiceImpl musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/")
    public String mainPage(Model model) {
        model.addAttribute("playlistData", musicService.findAll());
        return "main";
    }

    @GetMapping("/add")
    public String addPage() {
        return "playlist-create";
    }

    @GetMapping("/playlist")
    public String view(@RequestParam String id, @RequestParam String name, Model model) {
        ChzzkChannelConnectDto connectDto = musicService.chzzkChannelConnect(musicService.findByIdAndPlaylistName(id, name));

        model.addAttribute("playlistName", name);
        model.addAttribute("playlistId", connectDto.getPlaylistId());
        model.addAttribute("chatChannelId", connectDto.getChatChannelId());
        model.addAttribute("accessToken", connectDto.getAccessToken());
        model.addAttribute("serverId", connectDto.getServerId());

        return "music";
    }

}
