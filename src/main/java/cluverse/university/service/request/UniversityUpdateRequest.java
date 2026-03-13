package cluverse.university.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UniversityUpdateRequest(
        @NotBlank(message = "학교명을 입력해주세요.")
        @Size(max = 100, message = "학교명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 100, message = "학교 이메일 도메인은 100자 이하여야 합니다.")
        String emailDomain,

        @Size(max = 500, message = "학교 배지 이미지 URL은 500자 이하여야 합니다.")
        String badgeImageUrl,

        @Size(max = 300, message = "학교 주소는 300자 이하여야 합니다.")
        String address,

        @NotNull(message = "학교 활성 여부를 입력해주세요.")
        Boolean isActive
) {
}
