package cluverse.report.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReasonCode reasonCode;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @ElementCollection
    @CollectionTable(name = "report_evidence_image", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "image_url")
    private List<String> evidenceImageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.RECEIVED;

    @Builder(access = AccessLevel.PRIVATE)
    private Report(Long reporterId,
                   ReportTargetType targetType,
                   Long targetId,
                   ReportReasonCode reasonCode,
                   String detail,
                   List<String> evidenceImageUrls) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonCode = reasonCode;
        this.detail = detail;
        if (evidenceImageUrls != null) {
            this.evidenceImageUrls.addAll(evidenceImageUrls);
        }
    }

    public static Report create(Long reporterId,
                                ReportTargetType targetType,
                                Long targetId,
                                ReportReasonCode reasonCode,
                                String detail,
                                List<String> evidenceImageUrls) {
        return Report.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(targetId)
                .reasonCode(reasonCode)
                .detail(detail)
                .evidenceImageUrls(evidenceImageUrls)
                .build();
    }
}
