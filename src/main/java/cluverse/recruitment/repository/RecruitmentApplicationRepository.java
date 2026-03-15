package cluverse.recruitment.repository;

import cluverse.recruitment.domain.RecruitmentApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {

    Optional<RecruitmentApplication> findByRecruitmentIdAndApplicantId(Long recruitmentId, Long applicantId);

    List<RecruitmentApplication> findAllByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    List<RecruitmentApplication> findAllByRecruitmentIdOrderByCreatedAtDesc(Long recruitmentId);
}
