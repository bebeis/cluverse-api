package cluverse.notification.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String excerpt;

    @Column(nullable = false)
    private boolean isRead;

    private String targetUrl;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(Long memberId,
                         NotificationType type,
                         String title,
                         String content,
                         String excerpt,
                         boolean isRead,
                         String targetUrl) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.excerpt = excerpt;
        this.isRead = isRead;
        this.targetUrl = targetUrl;
    }

    public static Notification create(Long memberId,
                                      NotificationType type,
                                      String title,
                                      String content,
                                      String excerpt,
                                      String targetUrl) {
        return Notification.builder()
                .memberId(memberId)
                .type(type)
                .title(title)
                .content(content)
                .excerpt(excerpt)
                .isRead(false)
                .targetUrl(targetUrl)
                .build();
    }

    public void markRead() {
        this.isRead = true;
    }
}
