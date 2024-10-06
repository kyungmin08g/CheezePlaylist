package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDto;

public interface UserService {
    void joinUser(JoinMemberDto dto);

    void save(JoinMemberDto joinMemberDTO);
    JoinMemberDto findByUsername(String username);

    void refreshTokenSave(String username, String refreshToken);
    String refreshTokenFindByUsername(String username);
    void refreshTokenDeleteByUsername(String username);
}
