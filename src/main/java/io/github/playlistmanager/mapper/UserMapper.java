package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.dto.MusicFileDTO;
import io.github.playlistmanager.dto.PlaylistDTO;
import io.github.playlistmanager.dto.RoomDTO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserMapper {
    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);

    void mp3FileSave(MusicFileDTO musicFileDTO);
    MusicFileDTO findByTitle(int roomId, String title);
    List<MusicFileDTO> selectMusicFiles(int roomId);
    MusicFileDTO selectMusicFilesByTitle(String title);
    void deleteMusicFile(int roomId, String title);

    void roomSave(RoomDTO roomDTO);
    List<RoomDTO> selectAllRooms();
    RoomDTO selectRoomById(int roomId);
    void playlistSave(PlaylistDTO playlistDTO);

}
