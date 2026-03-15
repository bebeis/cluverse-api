package cluverse.recruitment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_chat_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_application_id", nullable = false)
    private RecruitmentApplication application;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "client_ip")
    private String clientIp;

    private ApplicationChatMessage(RecruitmentApplication application, Long senderId, String content, String clientIp) {
        this.application = application;
        this.senderId = senderId;
        this.content = content;
        this.clientIp = clientIp;
        this.isRead = false;
    }

    public static ApplicationChatMessage create(RecruitmentApplication application,
                                                Long senderId,
                                                String content,
                                                String clientIp) {
        return new ApplicationChatMessage(application, senderId, content, clientIp);
    }
}
