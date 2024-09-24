package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;

import java.io.IOException;
import java.util.List;

public interface UserService {
    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);

    void musicDownload(int roomId, String artist, String title);
    String searchVideo(String query);
    void downloadMusicFile(int roomId, String youtubeURL, String customTitle) throws IOException, InterruptedException;
    byte[] mp3Conversion(String youtubeURL) throws InterruptedException, IOException;

    void mp3FileSave(MusicFileDTO musicFileDTO);
    MusicFileDTO findByTitle(int roomId, String title);
    List<MusicFileDTO> selectMusicFiles(int roomId);
    MusicFileDTO selectMusicFilesByTitle(String title);
    void deleteMusicFile(int roomId, String title);
}
