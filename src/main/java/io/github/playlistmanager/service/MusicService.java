package io.github.playlistmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.playlistmanager.dto.MusicFileDTO;

import java.io.IOException;
import java.util.List;

public interface MusicService {
    void memberMusicDownload(int roomId, String artist, String title);
    void donationMusicDownload(int roomId, String donationContent);
    String calculateMonthsSubscribed(JsonNode streamingPropertyNode);
    void donationChat(int roomId, String chatJson);

    void musicDownload(int roomId, String artist, String title);
    String searchVideo(String query);
    void conversionAndDownload(int roomId, String youtubeUrl, String artist, String customTitle) throws IOException, InterruptedException;
    byte[] mp3Conversion(String youtubeUrl) throws InterruptedException, IOException;

    // Mapper 관련
    void save(MusicFileDTO musicFileDTO);
    MusicFileDTO findByMusic(int roomId, String artist, String title);
    List<MusicFileDTO> findById(int roomId);
    MusicFileDTO findByArtistAndTitle(String artist, String title);
    void delete(int roomId, String artist, String title);
}
