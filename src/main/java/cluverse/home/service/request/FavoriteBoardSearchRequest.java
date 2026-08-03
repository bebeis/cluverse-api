package cluverse.home.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record FavoriteBoardSearchRequest(
        @Positive(message = "커서는 0보다 커야 합니다.") Long cursor,
        @Min(value = 1, message = "조회 크기는 1 이상이어야 합니다.")
        @Max(value = 20, message = "조회 크기는 20 이하여야 합니다.") Integer size
) {
    private static final int DEFAULT_SIZE = 10;

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
