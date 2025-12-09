package com.banking.dto.auth;

public record AuthenticationResponse(
    String token,
    String refreshToken
) {
    public static AuthenticationResponse of(String token, String refreshToken) {
        return new AuthenticationResponse(token, refreshToken);
    }
}
