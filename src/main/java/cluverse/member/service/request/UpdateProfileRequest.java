package cluverse.member.service.request;

import cluverse.member.domain.MemberProfileField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(
        @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
        String bio,

        @Min(value = 1900, message = "입학년도는 1900년 이후여야 합니다.")
        @Max(value = 9999, message = "입학년도 형식이 올바르지 않습니다.")
        Integer entranceYear,

        String profileImageUrl,
        String linkGithub,
        String linkNotion,
        String linkPortfolio,
        String linkInstagram,
        String linkEtc,
        boolean isPublic,
        List<MemberProfileField> visibleFields
) {
}
