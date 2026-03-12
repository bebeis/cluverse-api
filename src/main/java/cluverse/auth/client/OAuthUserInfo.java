package cluverse.auth.client;

public record OAuthUserInfo(
        String providerId,
        String email,
        String nickname
) {}
