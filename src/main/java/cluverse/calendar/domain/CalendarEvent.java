package cluverse.calendar.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_event_id")
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarEventCategory category;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    private String location;

    @Column(nullable = false)
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarEventVisibility visibility;

    @Builder(access = AccessLevel.PRIVATE)
    private CalendarEvent(Long memberId,
                          String title,
                          String description,
                          CalendarEventCategory category,
                          LocalDateTime startAt,
                          LocalDateTime endAt,
                          String location,
                          boolean allDay,
                          CalendarEventVisibility visibility) {
        this.memberId = memberId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.allDay = allDay;
        this.visibility = visibility;
    }

    public static CalendarEvent create(Long memberId,
                                       String title,
                                       String description,
                                       CalendarEventCategory category,
                                       LocalDateTime startAt,
                                       LocalDateTime endAt,
                                       String location,
                                       boolean allDay,
                                       CalendarEventVisibility visibility) {
        return CalendarEvent.builder()
                .memberId(memberId)
                .title(title)
                .description(description)
                .category(category)
                .startAt(startAt)
                .endAt(endAt)
                .location(location)
                .allDay(allDay)
                .visibility(visibility)
                .build();
    }

    public void update(String title,
                       String description,
                       CalendarEventCategory category,
                       LocalDateTime startAt,
                       LocalDateTime endAt,
                       String location,
                       boolean allDay,
                       CalendarEventVisibility visibility) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.allDay = allDay;
        this.visibility = visibility;
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
