package cluverse.event.repository;

import cluverse.event.domain.CampusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CampusEventRepository extends JpaRepository<CampusEvent, Long> {

    @Query("""
            select campusEvent
            from CampusEvent campusEvent
            where (:ongoingOnly = false or (campusEvent.startDate <= :today and campusEvent.endDate >= :today))
              and (:from is null or campusEvent.endDate >= :from)
              and (:to is null or campusEvent.startDate <= :to)
            order by campusEvent.startDate asc, campusEvent.id asc
            """)
    List<CampusEvent> search(
            @Param("today") LocalDate today,
            @Param("ongoingOnly") boolean ongoingOnly,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
