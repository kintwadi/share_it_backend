package com.nearshare.api.dto;

import lombok.Getter;

@Getter
public class TokenResponse {
    private final String token;
    private final UserDTO user;
    private final boolean mfaRequired;

    public TokenResponse(String token, UserDTO user) {
        this(token, user, false);
    }

    public TokenResponse(String token, UserDTO user, boolean mfaRequired) {
        this.token = token;
        this.user = user;
        this.mfaRequired = mfaRequired;
    }
}
