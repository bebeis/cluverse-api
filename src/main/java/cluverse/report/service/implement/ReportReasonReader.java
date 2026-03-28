package cluverse.report.service.implement;

import cluverse.report.domain.ReportReasonCode;
import cluverse.report.service.response.ReportReasonResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Transactional(readOnly = true)
public class ReportReasonReader {

    public List<ReportReasonResponse> readReasonCodes() {
        return Arrays.stream(ReportReasonCode.values())
                .map(ReportReasonResponse::from)
                .toList();
    }
}
