package io.github.playlistmanager.mvc.service.impl;

import io.github.playlistmanager.mvc.mapper.MemberMapper;
import io.github.playlistmanager.mvc.service.MemberService;
import io.github.playlistmanager.mvc.dto.JoinMemberDTO;
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
