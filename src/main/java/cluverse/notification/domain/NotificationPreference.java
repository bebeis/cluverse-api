package cluverse.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private boolean comments = true;

    @Column(nullable = false)
    private boolean groups = true;

    @Column(nullable = false)
    private boolean announcements = true;

    @Column(nullable = false)
    private boolean follows = true;

    @Column(nullable = false)
    private boolean marketing = false;

    private NotificationPreference(Long memberId) {
        this.memberId = memberId;
    }

    public static NotificationPreference create(Long memberId) {
        return new NotificationPreference(memberId);
    }

    public void update(boolean comments, boolean groups, boolean announcements, boolean follows, boolean marketing) {
        this.comments = comments;
        this.groups = groups;
        this.announcements = announcements;
        this.follows = follows;
        this.marketing = marketing;
    }
}
