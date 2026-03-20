package cluverse.report.service.response;

import cluverse.report.domain.ReportReasonCode;

public record ReportReasonResponse(
        String reasonCode,
        String description
) {
    public static ReportReasonResponse from(ReportReasonCode reasonCode) {
        return new ReportReasonResponse(reasonCode.name(), reasonCode.getDescription());
    }
}
