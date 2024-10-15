package io.github.playlistmanager.service;

import io.github.playlistmanager.dto.JoinMemberDto;

public interface UserService {
    void joinUser(JoinMemberDto dto);
    String identicalUsername(String username);

    void save(JoinMemberDto joinMemberDTO);
    JoinMemberDto findByUsername(String username);
    JoinMemberDto findByEmail(String email);

    void refreshTokenSave(String username, String refreshToken);
    String refreshTokenFindByUsername(String username);
    void refreshTokenDeleteByUsername(String username);
}
