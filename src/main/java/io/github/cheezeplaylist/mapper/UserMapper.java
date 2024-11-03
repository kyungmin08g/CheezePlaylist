package io.github.cheezeplaylist.mapper;

import io.github.cheezeplaylist.dto.JoinMemberDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    void save(JoinMemberDto joinMemberDTO);
    JoinMemberDto findByUsername(String username);
    JoinMemberDto findByEmail(String email);

    void refreshTokenSave(String username, String refreshToken);
    String refreshTokenFindByUsername(String username);
    void refreshTokenDeleteByUsername(String username);
}
