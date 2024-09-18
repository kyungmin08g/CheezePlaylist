package io.github.playlistmanager.security.user.service;

import io.github.playlistmanager.security.user.dto.CustomUserDetails;
import io.github.playlistmanager.mvc.dto.JoinMemberDTO;
import io.github.playlistmanager.mvc.service.impl.MemberServiceImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberServiceImpl memberService;

    public CustomUserDetailsService(MemberServiceImpl memberService) {
        this.memberService = memberService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JoinMemberDTO memberDTO = memberService.selectMemberByUsername(username);

        if (Objects.equals(username, memberDTO.getUsername())) {
            return new CustomUserDetails(memberDTO);
        }

        return null;
    }
}
