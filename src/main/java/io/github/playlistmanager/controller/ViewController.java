package io.github.playlistmanager.controller;

import io.github.chzzkapi.api.ChzzkAPI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    @GetMapping("/{id}")
    public String index(@PathVariable("id") String id,  Model model) {
        model.addAttribute("id", id);

        String chatId = ChzzkAPI.getChatChannelId("");
        String accessToken = ChzzkAPI.getAccessToken(chatId);

        int serverId = 0;
        for (char i : chatId.toCharArray()) {
            serverId += Character.getNumericValue(i);
        }
        serverId = Math.abs(serverId) % 9 + 1;

        model.addAttribute("chatChannelId", chatId);
        model.addAttribute("accessToken", accessToken);
        model.addAttribute("serverId", serverId);

        return "Test";
    }

}
