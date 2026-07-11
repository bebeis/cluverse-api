package cluverse.report.service;

import cluverse.report.service.implement.ReportReasonReader;
import cluverse.report.service.response.ReportReasonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportQueryService {

    private final ReportReasonReader reportReasonReader;

    public List<ReportReasonResponse> getReasonCodes() {
        return reportReasonReader.readReasonCodes();
    }
}
