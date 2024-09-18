package io.github.playlistmanager.service.impl;

import io.github.playlistmanager.mapper.MemberMapper;
import io.github.playlistmanager.service.MemberService;
import io.github.playlistmanager.dto.JoinMemberDTO;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl implements MemberService {
    private final MemberMapper mapper;

    public MemberServiceImpl(MemberMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void signUp(JoinMemberDTO joinMemberDTO) {
        mapper.signUp(joinMemberDTO);
    }

    @Override
    public JoinMemberDTO selectMemberByUsername(String username) {
        return mapper.selectMemberByUsername(username);
    }
}
