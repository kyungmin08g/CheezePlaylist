package io.github.playlistmanager.security.filter;

import io.github.playlistmanager.provider.JwtProvider;
import io.github.playlistmanager.security.user.details.CustomUserDetails;
import io.github.playlistmanager.service.impl.UserServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class LoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserServiceImpl userService;

    public LoginAuthenticationFilter(
            JwtProvider jwtProvider,
            AuthenticationManager authenticationManager,
            UserServiceImpl userService
    ) {
        this.jwtProvider = jwtProvider;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(token);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) {
        log.info("로그인 성공");
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();

        String username = userDetails.getUsername();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        log.info("username: {}, role: {}", username, role);

        String accessToken = jwtProvider.createAccessToken(username, role);

        if (userService.refreshTokenFindByUsername(username) != null) {
            Cookie cookie = new Cookie("accessToken", accessToken);
            cookie.setMaxAge(3600);
            response.addCookie(cookie);
        } else {
            String refreshToken = jwtProvider.createRefreshToken(username, role);
            userService.refreshTokenSave(username, refreshToken);

            Cookie cookie = new Cookie("accessToken", accessToken);
            cookie.setMaxAge(3600);
            response.addCookie(cookie);
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.error("인증 실패: {}", failed.getMessage());
        response.sendRedirect("/signup");
    }
}
