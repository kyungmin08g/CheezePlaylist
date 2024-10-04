package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDTO;

public interface UserService {
    void save(JoinMemberDTO joinMemberDTO);
    JoinMemberDTO findByUsername(String username);

    void refreshTokenSave(String username, String refreshToken);
    String refreshTokenFindByUsername(String username);
    void refreshTokenDeleteByUsername(String username);
}
