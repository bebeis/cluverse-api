package cluverse.board.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardUpdateRequest(
        @NotBlank(message = "게시판명을 입력해주세요.")
        @Size(max = 100, message = "게시판명은 100자 이하여야 합니다.")
        String name,

        @Size(max = 3000, message = "게시판 설명은 3000자 이하여야 합니다.")
        String description,

        @Max(value = 9999, message = "노출 순서는 9999 이하여야 합니다.")
        Integer displayOrder,

        Boolean isActive
) {
    private static final int DEFAULT_DISPLAY_ORDER = 0;

    public int displayOrderOrDefault() {
        return displayOrder == null ? DEFAULT_DISPLAY_ORDER : displayOrder;
    }

    public boolean isActiveOrDefault() {
        return isActive == null || isActive;
    }
}
