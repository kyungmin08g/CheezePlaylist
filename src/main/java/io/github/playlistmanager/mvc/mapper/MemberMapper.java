package io.github.playlistmanager.mvc.mapper;

import io.github.playlistmanager.mvc.dto.JoinMemberDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper {

    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);

}
