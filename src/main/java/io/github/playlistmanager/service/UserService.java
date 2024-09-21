package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;

import java.io.IOException;

public interface UserService {
    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);

    void musicDownload(String artist, String title);
    String searchVideo(String query);
    byte[] mp3Conversion(String youtubeURL) throws InterruptedException, IOException;

    void mp3FileSave(MusicFileDTO musicFileDTO);
    MusicFileDTO findByTitle(String title);
}
