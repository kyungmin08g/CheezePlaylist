package io.github.playlistmanager.service.impl;

import io.github.playlistmanager.dto.JoinMemberDto;
import io.github.playlistmanager.mapper.UserMapper;
import io.github.playlistmanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
    public void joinUser(JoinMemberDto dto) {
        JoinMemberDto memberDto = JoinMemberDto.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .encryptionPassword(bCryptPasswordEncoder.encode(dto.getPassword()))
                .role("ROLE_USER").build();

        save(memberDto);
    }

    @Override
    public String identicalUsername(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username)).map(JoinMemberDto::getUsername).orElse(null);
    }

    @Override
    public void save(JoinMemberDto joinMemberDTO) {
        userMapper.save(joinMemberDTO);
    }

    @Override
    public JoinMemberDto findByUsername(String username) {
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
