package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.MusicFileDto;
import io.github.playlistmanager.dto.PlaylistDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MusicMapper {
    void save(MusicFileDto musicFileDTO);
    MusicFileDto findByMusic(String roomId, String artist, String title);
    List<MusicFileDto> findById(String roomId);
    MusicFileDto findByArtistAndTitle(String artist, String title);
    void delete(String roomId, String artist, String title);
    void deleteById(String playlistId);

    void saveChannelId(PlaylistDto dto);
    PlaylistDto findByPlaylistId(String playlistId);
    PlaylistDto findByIdAndPlaylistName(String playlistId, String playlistName, String username);
    List<PlaylistDto> findAll(String username);
    void playlistUpdate(String playlistId, String playlistName, String chzzkChannelId, String username, String donationPrice);
    void playlistDelete(String playlistId, String playlistName, String username);
}
