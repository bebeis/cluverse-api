package cluverse.member.service.request;

import cluverse.member.domain.MajorType;
import jakarta.validation.constraints.NotNull;

public record AddMajorRequest(
        @NotNull(message = "학과 ID를 입력해주세요.")
        Long majorId,

        @NotNull(message = "전공 유형을 선택해주세요.")
        MajorType majorType
) {
}
