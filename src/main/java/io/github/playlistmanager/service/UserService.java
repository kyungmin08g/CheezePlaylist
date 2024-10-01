package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDTO;

public interface UserService {
    void save(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO findByUsername(String username);
}
