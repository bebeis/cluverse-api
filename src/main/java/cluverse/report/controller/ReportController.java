package cluverse.report.controller;

import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import cluverse.report.service.ReportQueryService;
import cluverse.report.service.ReportService;
import cluverse.report.service.request.ReportCreateRequest;
import cluverse.report.service.response.ReportReasonResponse;
import cluverse.report.service.response.ReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportQueryService reportQueryService;

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportResponse> createReport(@Login LoginMember loginMember,
                                                    @RequestBody @Valid ReportCreateRequest request) {
        Long reporterId = loginMember == null ? null : loginMember.memberId();
        return ApiResponse.created(reportService.createReport(reporterId, request));
    }

    @GetMapping("/report-reasons")
    public ApiResponse<List<ReportReasonResponse>> getReportReasons() {
        return ApiResponse.ok(reportQueryService.getReasonCodes());
    }
}
