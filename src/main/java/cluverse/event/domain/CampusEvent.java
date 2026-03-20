package cluverse.event.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampusEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campus_event_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String location;

    private String thumbnailImageUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Builder(access = AccessLevel.PRIVATE)
    private CampusEvent(String title,
                        String host,
                        LocalDate startDate,
                        LocalDate endDate,
                        String location,
                        String thumbnailImageUrl,
                        String summary) {
        this.title = title;
        this.host = host;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.summary = summary;
    }

    public static CampusEvent create(String title,
                                     String host,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     String location,
                                     String thumbnailImageUrl,
                                     String summary) {
        return CampusEvent.builder()
                .title(title)
                .host(host)
                .startDate(startDate)
                .endDate(endDate)
                .location(location)
                .thumbnailImageUrl(thumbnailImageUrl)
                .summary(summary)
                .build();
    }
}
