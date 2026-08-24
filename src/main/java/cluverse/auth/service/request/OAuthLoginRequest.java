package cluverse.auth.service.request;

import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(
        @Size(max = 2048) String code,
        @Size(max = 4096) String accessToken,
        @Size(max = 500) String redirectUri
) {
}
