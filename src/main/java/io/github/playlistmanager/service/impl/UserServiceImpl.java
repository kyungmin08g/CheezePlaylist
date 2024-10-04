package io.github.playlistmanager.service.impl;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.mapper.UserMapper;
import io.github.playlistmanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void save(JoinMemberDTO joinMemberDTO) {
        userMapper.save(joinMemberDTO);
    }

    @Override
    public JoinMemberDTO findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public void refreshTokenSave(String username, String refreshToken) {
        userMapper.refreshTokenSave(username, refreshToken);
    }

    @Override
    public String refreshTokenFindByUsername(String username) {
        return userMapper.refreshTokenFindByUsername(username);
    }

    @Override
    public void refreshTokenDeleteByUsername(String username) {
        userMapper.refreshTokenDeleteByUsername(username);
    }
}
