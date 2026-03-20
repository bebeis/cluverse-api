package cluverse.report.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.report.domain.Report;
import cluverse.report.domain.ReportReasonCode;
import cluverse.report.repository.ReportRepository;
import cluverse.report.service.request.ReportCreateRequest;
import cluverse.report.service.response.ReportReasonResponse;
import cluverse.report.service.response.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportResponse createReport(Long reporterId, ReportCreateRequest request) {
        validateAuthenticated(reporterId);
        Report report = reportRepository.save(Report.create(
                reporterId,
                request.targetType(),
                request.targetId(),
                request.reasonCode(),
                request.detail(),
                request.evidenceImageUrls()
        ));
        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<ReportReasonResponse> getReasonCodes() {
        return Arrays.stream(ReportReasonCode.values())
                .map(ReportReasonResponse::from)
                .toList();
    }

    private void validateAuthenticated(Long reporterId) {
        if (reporterId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
