package io.github.playlistmanager.security.oauth2.user.response;

import io.github.playlistmanager.security.oauth2.OAuth2Response;

import java.util.Map;

public class OAuth2GoogleResponse implements OAuth2Response {

    private final Map<String, Object> attributes;

    public OAuth2GoogleResponse(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        return attributes.get("sub").toString();
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
