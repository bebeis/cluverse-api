package cluverse.calendar.repository;

import cluverse.calendar.domain.CalendarEvent;
import cluverse.calendar.domain.CalendarEventCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("""
            select calendarEvent
            from CalendarEvent calendarEvent
            where calendarEvent.memberId = :memberId
              and (:from is null or calendarEvent.endAt >= :from)
              and (:to is null or calendarEvent.startAt <= :to)
              and (:category is null or calendarEvent.category = :category)
            order by calendarEvent.startAt asc, calendarEvent.id asc
            """)
    List<CalendarEvent> search(
            @Param("memberId") Long memberId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("category") CalendarEventCategory category
    );

    List<CalendarEvent> findAllByMemberIdAndEndAtGreaterThanEqualOrderByStartAtAscIdAsc(
            Long memberId,
            LocalDateTime now,
            Pageable pageable
    );
}
