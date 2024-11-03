package io.github.cheezeplaylist.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cheezeplaylist.dto.ChzzkChannelConnectDto;
import io.github.cheezeplaylist.dto.MusicFileDto;
import io.github.cheezeplaylist.dto.PlaylistDto;

import java.io.IOException;
import java.util.List;

public interface MusicService {
    void memberMusicDownload(String roomId, String artist, String title);
    void donationMusicDownloader(String roomId, String artist, String title, String donationUsername, String donationPrice, String donationSubscriber);
    void donationMusicDownload(String roomId, String donationContent, String donationUsername, String donationPrice, String donationSubscriber);
    String calculateMonthsSubscribed(JsonNode streamingPropertyNode);
    void donationChat(String roomId, String chatJson);

    void musicDownload(String roomId, String artist, String title, String donationUsername, String donationPrice, String donationSubscriber);
    String searchVideo(String query);
    void conversionAndDownload(String roomId, String youtubeUrl, String artist, String customTitle, String donationUsername, String donationPrice, String donationSubscriber) throws IOException, InterruptedException;
    byte[] mp3Conversion(String youtubeUrl) throws InterruptedException, IOException;
    ChzzkChannelConnectDto chzzkChannelConnect(PlaylistDto playlistDto);
    byte[] spotifyMusicAlbum(String artist, String title);

    // Mapper 관련
    // 음악
    void save(MusicFileDto musicFileDTO);
    MusicFileDto findByMusic(String roomId, String artist, String title);
    List<MusicFileDto> findById(String roomId);
    MusicFileDto findByArtistAndTitle(String artist, String title);
    void delete(String roomId, String artist, String title);
    void deleteById(String playlistId);

    // 플레이리스트
    void saveChannelId(PlaylistDto dto);
    PlaylistDto findByPlaylistId(String playlistId);
    PlaylistDto findByIdAndPlaylistName(String playlistId, String playlistName, String username);
    List<PlaylistDto> findAll(String username);
    void playlistUpdate(String playlistId, String playlistName, String chzzkChannelId, String username, String donationPrice);
    void playlistDelete(String playlistId, String playlistName, String username);
}
