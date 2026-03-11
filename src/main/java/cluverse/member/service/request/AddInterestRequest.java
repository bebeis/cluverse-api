package cluverse.member.service.request;

import jakarta.validation.constraints.NotNull;

public record AddInterestRequest(
        @NotNull(message = "관심 태그 ID를 입력해주세요.")
        Long interestId
) {
}
