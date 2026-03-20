package cluverse.recruitment.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentApplicationRepository;
import cluverse.recruitment.repository.RecruitmentRepository;
import cluverse.recruitment.service.request.RecruitmentApplicationSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationReader {

    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final RecruitmentRepository recruitmentRepository;

    public RecruitmentApplication readOrThrow(Long applicationId) {
        return recruitmentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_NOT_FOUND.getMessage()
                ));
    }

    public Recruitment readRecruitmentOrThrow(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage()));
        validateActive(recruitment);
        return recruitment;
    }

    public List<RecruitmentApplication> readMyApplications(Long memberId, RecruitmentApplicationSearchRequest request) {
        return recruitmentApplicationRepository.findAllByApplicantIdOrderByCreatedAtDesc(memberId).stream()
                .filter(application -> request.status() == null || application.getStatus() == request.status())
                .toList();
    }

    public List<RecruitmentApplication> readApplications(Long recruitmentId, RecruitmentApplicationSearchRequest request) {
        return recruitmentApplicationRepository.findAllByRecruitmentIdOrderByCreatedAtDesc(recruitmentId).stream()
                .filter(application -> request.status() == null || application.getStatus() == request.status())
                .toList();
    }

    public boolean existsByRecruitmentAndApplicant(Long recruitmentId, Long applicantId) {
        return recruitmentApplicationRepository.findByRecruitmentIdAndApplicantId(recruitmentId, applicantId).isPresent();
    }

    public Map<Long, Recruitment> readRecruitmentMap(Collection<Long> recruitmentIds) {
        if (recruitmentIds == null || recruitmentIds.isEmpty()) {
            return Map.of();
        }
        return recruitmentRepository.findAllById(recruitmentIds).stream()
                .collect(Collectors.toMap(Recruitment::getId, recruitment -> recruitment));
    }

    private void validateActive(Recruitment recruitment) {
        if (recruitment.isDeleted()) {
            throw new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage());
        }
    }
}
