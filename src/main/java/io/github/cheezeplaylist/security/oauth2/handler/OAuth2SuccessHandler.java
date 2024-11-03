package io.github.cheezeplaylist.security.oauth2.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cheezeplaylist.provider.JwtProvider;
import io.github.cheezeplaylist.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static String provider;

    private final String host;
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final UserService userService;

    public OAuth2SuccessHandler(@Value("${server.host}") String host, JwtProvider jwtProvider, UserService userService) {
        this.host = host;
        this.objectMapper = new ObjectMapper();
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OAuth2 인증에 성공");
        String attributesString = this.objectMapper.writeValueAsString(authentication.getPrincipal());
        JsonNode attributesNode = this.objectMapper.readTree(attributesString);

        switch (provider) {
            case "google":
                for (JsonNode authorities : attributesNode.path("authorities")) {
                    JsonNode attributes = authorities.path("attributes");

                    String sub = attributes.path("sub").asText();
                    String username = attributes.path("name").asText();
                    String email = attributes.path("email").asText();

                    log.info("\u001B[34m제공자: {}, 닉네임: {}, 이메일: {}\u001B[0m", provider, username, email);
                    sendAccessToken(username, sub, response);
                    return;
                }
            case "naver":
                for (JsonNode authorities : attributesNode.path("authorities")) {
                    JsonNode attributes = authorities.path("attributes");
                    JsonNode responseInfo = attributes.get("response");

                    String id = responseInfo.path("id").asText();
                    String username = responseInfo.path("name").asText();
                    String email = responseInfo.path("email").asText();

                    log.info("\u001B[34m제공자: {}, 닉네임: {}, 이메일: {}\u001B[0m", provider, username, email);
                    sendAccessToken(username, id, response);
                    return;
                }
            case "kakao":
                for (JsonNode authorities : attributesNode.path("authorities")) {
                    JsonNode attributes = authorities.path("attributes");
                    JsonNode kakao_account = attributes.path("kakao_account");
                    JsonNode profile = kakao_account.path("profile");

                    String id = attributes.path("id").asText();
                    String username = profile.path("nickname").asText();
                    String email = kakao_account.path("email").asText();

                    log.info("\u001B[34m제공자: {}, 닉네임: {}, 이메일: {}\u001B[0m", provider, username, email);
                    sendAccessToken(username, id, response);
                    return;
                }
            case "facebook":
                for (JsonNode authorities : attributesNode.path("authorities")) {
                    JsonNode attributes = authorities.path("attributes");

                    String sub = attributes.path("id").asText();
                    String username = attributes.path("name").asText();
                    String email = attributes.path("email").asText();

                    log.info("\u001B[34m제공자: {}, 닉네임: {}, 이메일: {}\u001B[0m", provider, username, email);
                    sendAccessToken(username, sub, response);
                    return;
                }
        }
    }

    private void sendAccessToken(String username, String id, HttpServletResponse response) throws IOException {
        String name = provider + "_" + id + "_" + username;

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(name, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        String refreshToken = jwtProvider.createRefreshToken(name, "ROLE_USER");
        userService.refreshTokenSave(name, refreshToken);

        String accessToken = jwtProvider.createAccessToken(name, "ROLE_USER");

        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setMaxAge(Integer.MAX_VALUE);
//        accessTokenCookie.setSecure(true);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);
        response.sendRedirect(host + "/");
    }

}
