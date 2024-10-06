package io.github.playlistmanager.security.oauth2.user.response;

import io.github.playlistmanager.security.oauth2.OAuth2Response;

import java.util.Map;

public class OAuth2KakaoResponse implements OAuth2Response {

    private final Map<String, Object> attributes;

    public OAuth2KakaoResponse(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return properties.get("nickname").toString();
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakao_account = (Map<String, Object>) attributes.get("kakao_account");
        return kakao_account.get("email").toString();
    }
}
