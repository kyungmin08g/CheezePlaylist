package io.github.playlistmanager.security.oauth2.user.response;

import io.github.playlistmanager.security.oauth2.OAuth2Response;

import java.util.Map;

public class OAuth2NaverResponse implements OAuth2Response {

    private final Map<String, Object> attributes;

    public OAuth2NaverResponse(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProvider() {
        return "Naver";
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getName() {
        return attributes.get("name").toString();
    }

    @Override
    public String getEmail() {
        return attributes.get("email").toString();
    }
}
