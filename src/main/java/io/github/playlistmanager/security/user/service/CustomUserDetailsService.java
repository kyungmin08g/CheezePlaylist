package io.github.playlistmanager.security.user.service;

import io.github.playlistmanager.security.user.details.CustomUserDetails;
import io.github.playlistmanager.dto.JoinMemberDTO;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserServiceImpl service;

    public CustomUserDetailsService(UserServiceImpl service) {
        this.service = service;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        JoinMemberDTO memberDTO = service.findByUsername(username);

        if (memberDTO != null && Objects.equals(username, memberDTO.getUsername())) {
            return new CustomUserDetails(memberDTO);
        }

        if (username.matches("^(.*)_(.*)_(.*)$")) {
            JoinMemberDTO oauth2MemberDto = JoinMemberDTO.builder()
                    .username(username)
                    .role("ROLE_USER")
                    .build();

            return new CustomUserDetails(oauth2MemberDto);
        }

        throw new UsernameNotFoundException(username + " 회원을 찾을 수 없습니다.");
    }
}
