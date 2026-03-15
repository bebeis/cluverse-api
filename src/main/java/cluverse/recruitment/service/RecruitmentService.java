package cluverse.recruitment.service;

import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentSearchRequest;
import cluverse.recruitment.service.request.RecruitmentStatusUpdateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import cluverse.recruitment.service.response.RecruitmentPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecruitmentService {

    @Transactional(readOnly = true)
    public RecruitmentPageResponse getRecruitments(Long memberId, RecruitmentSearchRequest request) {
        throw unsupported();
    }

    public RecruitmentDetailResponse createRecruitment(Long memberId, Long groupId, RecruitmentCreateRequest request) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public RecruitmentDetailResponse getRecruitment(Long memberId, Long recruitmentId) {
        throw unsupported();
    }

    public RecruitmentDetailResponse updateRecruitment(Long memberId,
                                                       Long recruitmentId,
                                                       RecruitmentUpdateRequest request) {
        throw unsupported();
    }

    public RecruitmentDetailResponse updateRecruitmentStatus(Long memberId,
                                                             Long recruitmentId,
                                                             RecruitmentStatusUpdateRequest request) {
        throw unsupported();
    }

    public void deleteRecruitment(Long memberId, Long recruitmentId) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Recruitment API contract only. Service implementation is pending.");
    }
}
