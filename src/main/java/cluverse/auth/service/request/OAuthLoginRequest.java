package cluverse.auth.service.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String code
) {
}
