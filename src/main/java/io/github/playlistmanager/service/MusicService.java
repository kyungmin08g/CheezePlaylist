package io.github.playlistmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.playlistmanager.dto.ChzzkChannelConnectDto;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.dto.PlaylistDto;

import java.io.IOException;
import java.util.List;

public interface MusicService {
    void memberMusicDownload(String roomId, String artist, String title);
    void donationMusicDownload(String roomId, String donationContent);
    String calculateMonthsSubscribed(JsonNode streamingPropertyNode);
    void donationChat(String roomId, String chatJson);

    void musicDownload(String roomId, String artist, String title);
    String searchVideo(String query);
    void conversionAndDownload(String roomId, String youtubeUrl, String artist, String customTitle) throws IOException, InterruptedException;
    byte[] mp3Conversion(String youtubeUrl) throws InterruptedException, IOException;
    ChzzkChannelConnectDto chzzkChannelConnect(PlaylistDto playlistDto);

    // Mapper 관련
    // 음악
    void save(MusicFileDTO musicFileDTO);
    MusicFileDTO findByMusic(String roomId, String artist, String title);
    List<MusicFileDTO> findById(String roomId);
    MusicFileDTO findByArtistAndTitle(String artist, String title);
    void delete(String roomId, String artist, String title);
    void deleteById(String playlistId);

    // 플레이리스트
    void saveChannelId(PlaylistDto dto);
    PlaylistDto findByIdAndPlaylistName(String playlistId, String playlistName, String username);
    List<PlaylistDto> findAll(String username);
    void playlistUpdate(String playlistId, String playlistName, String chzzkChannelId, String username);
    void playlistDelete(String playlistId, String playlistName, String username);
}
