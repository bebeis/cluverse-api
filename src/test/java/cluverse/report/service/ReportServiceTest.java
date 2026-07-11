package cluverse.report.service;

import cluverse.common.exception.UnauthorizedException;
import cluverse.report.domain.Report;
import cluverse.report.domain.ReportReasonCode;
import cluverse.report.domain.ReportTargetType;
import cluverse.report.service.implement.ReportWriter;
import cluverse.report.service.request.ReportCreateRequest;
import cluverse.report.service.response.ReportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportWriter reportWriter;

    @InjectMocks
    private ReportService reportService;

    @Test
    void 신고_생성은_Writer로_위임하고_응답으로_변환한다() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.POST,
                55L,
                ReportReasonCode.SPAM,
                "스팸 게시글입니다.",
                List.of("https://cdn.example.com/evidence1.png")
        );
        Report report = createReport(7L, request);
        when(reportWriter.create(200L, request)).thenReturn(report);

        // when
        ReportResponse response = reportService.createReport(200L, request);

        // then
        assertThat(response.reportId()).isEqualTo(7L);
        assertThat(response.targetType()).isEqualTo(ReportTargetType.POST);
        assertThat(response.targetId()).isEqualTo(55L);
        assertThat(response.reasonCode()).isEqualTo(ReportReasonCode.SPAM);
        assertThat(response.evidenceImageUrls()).containsExactly("https://cdn.example.com/evidence1.png");
    }

    @Test
    void 비로그인_사용자는_신고할_수_없다() {
        // given
        ReportCreateRequest request = new ReportCreateRequest(
                ReportTargetType.POST,
                55L,
                ReportReasonCode.SPAM,
                null,
                List.of()
        );

        // when, then
        assertThatThrownBy(() -> reportService.createReport(null, request))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(reportWriter);
    }

    private Report createReport(Long reportId, ReportCreateRequest request) {
        Report report = Report.create(
                200L,
                request.targetType(),
                request.targetId(),
                request.reasonCode(),
                request.detail(),
                request.evidenceImageUrls()
        );
        ReflectionTestUtils.setField(report, "id", reportId);
        return report;
    }
}
