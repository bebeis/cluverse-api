package cluverse.report.service.implement;

import cluverse.report.domain.Report;
import cluverse.report.repository.ReportRepository;
import cluverse.report.service.request.ReportCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ReportWriter {

    private final ReportRepository reportRepository;

    public Report create(Long reporterId, ReportCreateRequest request) {
        return reportRepository.save(Report.create(
                reporterId,
                request.targetType(),
                request.targetId(),
                request.reasonCode(),
                request.detail(),
                request.evidenceImageUrls()
        ));
    }
}
