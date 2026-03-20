package cluverse.member.service.request;

import jakarta.validation.constraints.NotNull;

public record MemberUniversityUpdateRequest(
        @NotNull(message = "학교 ID는 필수입니다.")
        Long universityId
) {
}
