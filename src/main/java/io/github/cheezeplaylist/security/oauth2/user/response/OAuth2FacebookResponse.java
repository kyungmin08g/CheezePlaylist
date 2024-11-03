package io.github.cheezeplaylist.security.oauth2.user.response;

import io.github.cheezeplaylist.security.oauth2.OAuth2Response;

import java.util.Map;

public class OAuth2FacebookResponse implements OAuth2Response {

    Map<String, Object> attributes;

    public OAuth2FacebookResponse(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "facebook";
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
