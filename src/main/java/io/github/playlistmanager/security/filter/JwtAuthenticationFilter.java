package io.github.playlistmanager.security.filter;

import io.github.playlistmanager.provider.JwtProvider;
import io.github.playlistmanager.security.user.details.CustomUserDetails;
import io.github.playlistmanager.security.user.service.CustomUserDetailsService;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import jakarta.annotation.Nullable;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserServiceImpl userService;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            UserServiceImpl userService
    ) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain) throws ServletException, IOException {
        String accessTokenKey = null;
        String accessToken = null;

        Cookie[] cookies = Objects.requireNonNull(request).getCookies();

        if (cookies == null) {
            Objects.requireNonNull(filterChain).doFilter(request, response);
            return;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("accessToken")) {
                accessTokenKey = cookie.getName();
                accessToken = cookie.getValue();
            }
        }

        if (accessToken == null) {
            Objects.requireNonNull(filterChain).doFilter(request, response);
            return;
        }

        // 액세스 토큰은 있는데 리프레쉬 토큰이 없을때
        if (userService.refreshTokenFindByUsername(jwtProvider.getUsername(accessToken)) == null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("accessToken")) {
                    cookie.setMaxAge(0);
                    Objects.requireNonNull(response).addCookie(cookie);
                    Objects.requireNonNull(filterChain).doFilter(request, response);
                    return;
                }
            }
        }

        // 액세스 토큰 재발급
        if (!jwtProvider.getValidateToken(accessToken)) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("accessToken")) {
                    cookie.setMaxAge(0);
                    Objects.requireNonNull(response).addCookie(cookie);

                    String refreshToken = userService.refreshTokenFindByUsername(jwtProvider.getUsername(accessToken));
                    if (jwtProvider.getValidateToken(refreshToken)) {
                        System.out.println("액세스 토큰 재발급");
                        updateAccessToken(accessToken, response);
                    } else {
                        userService.refreshTokenDeleteByUsername(jwtProvider.getUsername(accessToken));
                        Objects.requireNonNull(filterChain).doFilter(request, response);
                        return;
                    }

                }
            }
        }

        if (accessTokenKey.startsWith("accessToken") && jwtProvider.getValidateToken(accessToken)) {
            CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(userService);
            CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(jwtProvider.getUsername(accessToken));

            String username = userDetails.getUsername();
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            Objects.requireNonNull(filterChain).doFilter(request, response);
            return;
        }

        Objects.requireNonNull(filterChain).doFilter(request, response);
    }

    public void updateAccessToken(String token, HttpServletResponse response) {
        String username = jwtProvider.getUsername(token);
        String role = jwtProvider.getRole(token);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        String accessToken = jwtProvider.createAccessToken(username, role);

        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setMaxAge(Integer.MAX_VALUE);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
