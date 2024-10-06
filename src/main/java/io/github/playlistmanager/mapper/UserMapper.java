package io.github.playlistmanager.mapper;

import io.github.playlistmanager.dto.JoinMemberDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    void save(JoinMemberDto joinMemberDTO);
    JoinMemberDto findByUsername(String username);

    void refreshTokenSave(String username, String refreshToken);
    String refreshTokenFindByUsername(String username);
    void refreshTokenDeleteByUsername(String username);
}
