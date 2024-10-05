package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.dto.PlaylistDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MusicMapper {
    void save(MusicFileDTO musicFileDTO);
    MusicFileDTO findByMusic(String roomId, String artist, String title);
    List<MusicFileDTO> findById(String roomId);
    MusicFileDTO findByArtistAndTitle(String artist, String title);
    void delete(String roomId, String artist, String title);
    void deleteById(String playlistId);

    void saveChannelId(PlaylistDto dto);
    PlaylistDto findByIdAndPlaylistName(String playlistId, String playlistName, String username);
    List<PlaylistDto> findAll(String username);
    void playlistUpdate(String playlistId, String playlistName, String chzzkChannelId, String username);
    void playlistDelete(String playlistId, String playlistName, String username);
}
