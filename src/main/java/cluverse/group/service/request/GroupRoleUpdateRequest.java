package cluverse.group.service.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupRoleUpdateRequest(
        @NotBlank(message = "직책명을 입력해주세요.")
        @Size(max = 50, message = "직책명은 50자 이하여야 합니다.")
        String title,

        @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다.")
        @Max(value = 999, message = "표시 순서는 999 이하여야 합니다.")
        Integer displayOrder
) {
    private static final int DEFAULT_DISPLAY_ORDER = 0;

    public int displayOrderOrDefault() {
        return displayOrder == null ? DEFAULT_DISPLAY_ORDER : displayOrder;
    }
}
