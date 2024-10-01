package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.JoinMemberDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    void save(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO findByUsername(String username);
}
