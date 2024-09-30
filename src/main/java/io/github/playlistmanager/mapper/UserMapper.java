package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserMapper {
    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);

    void mp3FileSave(MusicFileDTO musicFileDTO);
    MusicFileDTO findByMusic(int roomId, String artist, String title);
    List<MusicFileDTO> selectMusicFiles(int roomId);
    MusicFileDTO selectMusicFilesByMusic(String artist, String title);
    void deleteMusicFile(int roomId, String artist, String title);
}
