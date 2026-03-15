package cluverse.recruitment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_application_id", nullable = false)
    private RecruitmentApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private RecruitmentApplicationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private RecruitmentApplicationStatus newStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "client_ip")
    private String clientIp;

    private ApplicationStatusHistory(RecruitmentApplication application,
                                     RecruitmentApplicationStatus previousStatus,
                                     RecruitmentApplicationStatus newStatus,
                                     Long changedBy,
                                     String note,
                                     String clientIp) {
        this.application = application;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.note = note;
        this.sourceSystem = "API";
        this.clientIp = clientIp;
    }

    public static ApplicationStatusHistory create(RecruitmentApplication application,
                                                  RecruitmentApplicationStatus previousStatus,
                                                  RecruitmentApplicationStatus newStatus,
                                                  Long changedBy,
                                                  String note,
                                                  String clientIp) {
        return new ApplicationStatusHistory(application, previousStatus, newStatus, changedBy, note, clientIp);
    }
}
