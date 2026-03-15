package cluverse.group.service.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupOwnerTransferRequest(
        @NotNull(message = "새 오너 회원 ID를 입력해주세요.")
        Long newOwnerMemberId,

        @Size(max = 1000, message = "사유는 1000자 이하여야 합니다.")
        String reason
) {
}
