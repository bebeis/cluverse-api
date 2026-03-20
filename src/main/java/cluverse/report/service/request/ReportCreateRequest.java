package cluverse.report.service.request;

import cluverse.report.domain.ReportReasonCode;
import cluverse.report.domain.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReportCreateRequest(
        @NotNull(message = "신고 대상을 입력해주세요.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상 ID를 입력해주세요.")
        Long targetId,

        @NotNull(message = "신고 사유를 입력해주세요.")
        ReportReasonCode reasonCode,

        @Size(max = 2000, message = "상세 설명은 2000자 이하여야 합니다.")
        String detail,

        @Size(max = 3, message = "증빙 이미지는 최대 3장까지 첨부할 수 있습니다.")
        List<String> evidenceImageUrls
) {
}
