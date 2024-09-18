package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDTO;

public interface MemberService {
    void signUp(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO selectMemberByUsername(String username);
}
