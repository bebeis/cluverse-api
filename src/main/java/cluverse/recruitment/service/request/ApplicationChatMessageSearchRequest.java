package cluverse.recruitment.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ApplicationChatMessageSearchRequest(
        Long beforeMessageId,

        @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "조회 건수는 100 이하여야 합니다.")
        Integer limit
) {
    private static final int DEFAULT_LIMIT = 50;

    public int limitOrDefault() {
        return limit == null ? DEFAULT_LIMIT : limit;
    }
}
