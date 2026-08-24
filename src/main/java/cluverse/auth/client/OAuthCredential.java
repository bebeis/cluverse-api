package cluverse.auth.client;

public record OAuthCredential(
        String code,
        String accessToken,
        String redirectUri
) {
}
