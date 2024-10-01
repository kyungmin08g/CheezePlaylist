package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.MusicFileDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MusicMapper {
    void save(MusicFileDTO musicFileDTO);
    MusicFileDTO findByMusic(int roomId, String artist, String title);
    List<MusicFileDTO> findById(int roomId);
    MusicFileDTO findByArtistAndTitle(String artist, String title);
    void delete(int roomId, String artist, String title);
}
