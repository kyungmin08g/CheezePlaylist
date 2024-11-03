package io.github.cheezeplaylist.security.oauth2.user.service.impl;

import io.github.cheezeplaylist.security.oauth2.OAuth2Response;
import io.github.cheezeplaylist.security.oauth2.handler.OAuth2SuccessHandler;
import io.github.cheezeplaylist.security.oauth2.user.response.OAuth2FacebookResponse;
import io.github.cheezeplaylist.security.oauth2.user.response.OAuth2GoogleResponse;
import io.github.cheezeplaylist.security.oauth2.user.response.OAuth2KakaoResponse;
import io.github.cheezeplaylist.security.oauth2.user.response.OAuth2NaverResponse;
import io.github.cheezeplaylist.security.oauth2.user.service.CustomOAuth2User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        switch (registrationId) {
            case "google" -> {
                oAuth2User(new OAuth2GoogleResponse(oAuth2User.getAttributes()));
            }
            case "naver" -> {
                oAuth2User(new OAuth2NaverResponse(oAuth2User.getAttributes()));
            }
            case "kakao" -> {
                oAuth2User(new OAuth2KakaoResponse(oAuth2User.getAttributes()));
            }
            case "facebook" -> {
                oAuth2User(new OAuth2FacebookResponse(oAuth2User.getAttributes()));
            }
        }

        return oAuth2User;
    }

    private CustomOAuth2User oAuth2User(OAuth2Response oAuth2Response) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", oAuth2Response.getName());
        attributes.put("role", "ROLE_USER");

        OAuth2SuccessHandler.provider = oAuth2Response.getProvider();

        return new CustomOAuth2User(attributes);
    }

}
