package cluverse.recruitment.service.implement;

import cluverse.common.exception.NotFoundException;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentRepository;
import cluverse.recruitment.service.request.RecruitmentSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentReader {

    private final RecruitmentRepository recruitmentRepository;

    public Recruitment readOrThrow(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage()));
        validateActive(recruitment);
        return recruitment;
    }

    public List<Recruitment> readRecruitments(RecruitmentSearchRequest request) {
        return recruitmentRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(recruitment -> request.groupId() == null || recruitment.getGroupId().equals(request.groupId()))
                .filter(recruitment -> request.status() == null || recruitment.getStatus() == request.status())
                .filter(recruitment -> !Boolean.TRUE.equals(request.recruitingOnly()) || recruitment.isOpen())
                .toList();
    }

    private void validateActive(Recruitment recruitment) {
        if (recruitment.isDeleted()) {
            throw new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_NOT_FOUND.getMessage());
        }
    }
}
