package cluverse.recruitment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_application_id")
    private Long id;

    @Column(name = "recruitment_id", nullable = false)
    private Long recruitmentId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    private String position;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentApplicationStatus status;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormItemAnswer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ApplicationStatusHistory> statusHistories = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ApplicationChatMessage> messages = new ArrayList<>();

    private RecruitmentApplication(Long recruitmentId, Long applicantId, String position, String portfolioUrl) {
        this.recruitmentId = recruitmentId;
        this.applicantId = applicantId;
        this.position = position;
        this.portfolioUrl = portfolioUrl;
        this.status = RecruitmentApplicationStatus.SUBMITTED;
    }

    public static RecruitmentApplication create(Long recruitmentId,
                                                Long applicantId,
                                                String position,
                                                String portfolioUrl,
                                                List<FormItemAnswer> answers,
                                                String clientIp) {
        RecruitmentApplication application = new RecruitmentApplication(recruitmentId, applicantId, position, portfolioUrl);
        application.replaceAnswers(answers);
        application.statusHistories.add(
                ApplicationStatusHistory.create(application, RecruitmentApplicationStatus.SUBMITTED,
                        RecruitmentApplicationStatus.SUBMITTED, applicantId, null, clientIp)
        );
        return application;
    }

    public void changeStatus(RecruitmentApplicationStatus newStatus, Long changedBy, String note, String clientIp) {
        RecruitmentApplicationStatus previousStatus = this.status;
        this.status = newStatus;
        this.reviewedBy = changedBy;
        this.reviewedAt = LocalDateTime.now();
        this.statusHistories.add(ApplicationStatusHistory.create(this, previousStatus, newStatus, changedBy, note, clientIp));
    }

    public ApplicationChatMessage addMessage(Long senderId, String content, String clientIp) {
        ApplicationChatMessage message = ApplicationChatMessage.create(this, senderId, content, clientIp);
        this.messages.add(message);
        return message;
    }

    public String getLatestReviewNote() {
        return statusHistories.stream()
                .map(ApplicationStatusHistory::getNote)
                .filter(note -> note != null && !note.isBlank())
                .findFirst()
                .orElse(null);
    }

    private void replaceAnswers(List<FormItemAnswer> answers) {
        this.answers.clear();
        if (answers == null) {
            return;
        }
        answers.forEach(answer -> {
            answer.assignApplication(this);
            this.answers.add(answer);
        });
    }
}
