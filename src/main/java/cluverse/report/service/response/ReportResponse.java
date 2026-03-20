package cluverse.report.service.response;

import cluverse.report.domain.Report;
import cluverse.report.domain.ReportReasonCode;
import cluverse.report.domain.ReportStatus;
import cluverse.report.domain.ReportTargetType;

import java.util.List;

public record ReportResponse(
        Long reportId,
        ReportTargetType targetType,
        Long targetId,
        ReportReasonCode reasonCode,
        String detail,
        List<String> evidenceImageUrls,
        ReportStatus status
) {
    public ReportResponse {
        evidenceImageUrls = evidenceImageUrls == null ? List.of() : List.copyOf(evidenceImageUrls);
    }

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReasonCode(),
                report.getDetail(),
                report.getEvidenceImageUrls(),
                report.getStatus()
        );
    }
}
