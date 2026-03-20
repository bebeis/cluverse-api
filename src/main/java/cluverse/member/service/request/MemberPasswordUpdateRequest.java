package cluverse.member.service.request;

import jakarta.validation.constraints.NotBlank;

public record MemberPasswordUpdateRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        String newPassword
) {
}
