package io.github.playlistmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    @GetMapping("/{id}")
    public String index(@PathVariable("id") String id, Model model) {
        model.addAttribute("id", id);
        return "Test";
    }

}
