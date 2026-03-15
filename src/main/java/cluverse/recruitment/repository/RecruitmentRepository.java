package cluverse.recruitment.repository;

import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    long countByGroupIdAndStatusAndDeletedAtIsNull(Long groupId, RecruitmentStatus status);

    List<Recruitment> findAllByDeletedAtIsNullOrderByCreatedAtDesc();
}
