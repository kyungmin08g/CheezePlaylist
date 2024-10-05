package io.github.playlistmanager.service.impl;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.mapper.UserMapper;
import io.github.playlistmanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserServiceImpl(UserMapper userMapper, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userMapper = userMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public void joinUser(JoinMemberDTO dto) {
        JoinMemberDTO memberDto = JoinMemberDTO.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .role("ROLE_USER").build();

        save(memberDto);
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
