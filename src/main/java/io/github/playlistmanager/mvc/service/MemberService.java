package io.github.playlistmanager.mvc.service;

import io.github.playlistmanager.mvc.dto.JoinMemberDTO;

public interface MemberService {
    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);
}
