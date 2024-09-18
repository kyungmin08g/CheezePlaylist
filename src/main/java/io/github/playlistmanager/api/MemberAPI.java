package io.github.playlistmanager.api;

import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.service.impl.MemberServiceImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class MemberAPI {
    private static MemberServiceImpl memberService;
    private static BCryptPasswordEncoder bCryptPasswordEncoder;

    public MemberAPI(MemberServiceImpl memberService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        MemberAPI.memberService = memberService;
        MemberAPI.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public static void signUp(String username, String email, String password) {
        JoinMemberDTO memberDTO = JoinMemberDTO.builder()
                .username(username)
                .email(email)
                .password(bCryptPasswordEncoder.encode(password))
                .role("USER_ROLE").build();
        memberService.signUp(memberDTO);
    }

    public static JoinMemberDTO selectMember(String username) {
        return memberService.selectMemberByUsername(username);
    }
}
