package io.github.playlistmanager.service.impl;

import io.github.playlistmanager.api.MemberAPI;
import io.github.playlistmanager.dto.CustomUserDetails;
import io.github.playlistmanager.dto.JoinMemberDTO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JoinMemberDTO memberDTO = MemberAPI.selectMember(username);

        if (Objects.equals(username, memberDTO.getUsername())) {
            return new CustomUserDetails(memberDTO);
        }

        return null;
    }
}
