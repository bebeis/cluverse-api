package cluverse.member.service.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
        String bio,

        String profileImageUrl,
        String linkGithub,
        String linkNotion,
        String linkPortfolio,
        String linkInstagram,
        String linkEtc,
        boolean isPublic,
        String visibleFields
) {
}
