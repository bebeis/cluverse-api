package cluverse.group.service.request;

import cluverse.group.domain.GroupMemberRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupMemberUpdateRequest(
        @NotNull(message = "변경할 그룹 역할을 선택해주세요.")
        GroupMemberRole role,

        Long customTitleId,

        @Size(max = 1000, message = "사유는 1000자 이하여야 합니다.")
        String reason
) {
}
