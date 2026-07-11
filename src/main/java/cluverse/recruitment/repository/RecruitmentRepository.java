package cluverse.recruitment.repository;

import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    long countByGroupIdAndStatusAndDeletedAtIsNull(Long groupId, RecruitmentStatus status);

    List<Recruitment> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Recruitment recruitment
            SET recruitment.applicationCount = recruitment.applicationCount + 1
            WHERE recruitment.id = :recruitmentId
            """)
    void increaseApplicationCount(@Param("recruitmentId") Long recruitmentId);
}
