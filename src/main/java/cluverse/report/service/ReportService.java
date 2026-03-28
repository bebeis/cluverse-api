package cluverse.report.service;

import cluverse.auth.exception.AuthExceptionMessage;
import cluverse.common.exception.UnauthorizedException;
import cluverse.report.domain.Report;
import cluverse.report.service.implement.ReportWriter;
import cluverse.report.service.request.ReportCreateRequest;
import cluverse.report.service.response.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportWriter reportWriter;

    public ReportResponse createReport(Long reporterId, ReportCreateRequest request) {
        validateAuthenticated(reporterId);
        Report report = reportWriter.create(reporterId, request);
        return ReportResponse.from(report);
    }

    private void validateAuthenticated(Long reporterId) {
        if (reporterId == null) {
            throw new UnauthorizedException(AuthExceptionMessage.UNAUTHORIZED.getMessage());
        }
    }
}
