package cluverse.recruitment.repository;

import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    long countByGroupIdAndStatusAndDeletedAtIsNull(Long groupId, RecruitmentStatus status);

    List<Recruitment> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    @Query("""
            SELECT recruitment
            FROM Recruitment recruitment
            LEFT JOIN FETCH recruitment.formItems
            WHERE recruitment.id = :recruitmentId
            """)
    Optional<Recruitment> findWithFormItemsById(@Param("recruitmentId") Long recruitmentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Recruitment recruitment
            SET recruitment.applicationCount = recruitment.applicationCount + 1
            WHERE recruitment.id = :recruitmentId
            """)
    void increaseApplicationCount(@Param("recruitmentId") Long recruitmentId);
}
