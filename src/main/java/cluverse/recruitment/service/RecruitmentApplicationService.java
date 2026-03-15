package cluverse.recruitment.service;

import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.ApplicationChatMessageSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import cluverse.recruitment.service.response.ApplicationChatMessagePageResponse;
import cluverse.recruitment.service.response.ApplicationChatMessageResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationDetailResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecruitmentApplicationService {

    @Transactional(readOnly = true)
    public RecruitmentApplicationPageResponse getMyApplications(Long memberId,
                                                               RecruitmentApplicationSearchRequest request) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public RecruitmentApplicationPageResponse getApplications(Long memberId,
                                                              Long recruitmentId,
                                                              RecruitmentApplicationSearchRequest request) {
        throw unsupported();
    }

    public RecruitmentApplicationDetailResponse createApplication(Long memberId,
                                                                  Long recruitmentId,
                                                                  RecruitmentApplicationCreateRequest request,
                                                                  String clientIp) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public RecruitmentApplicationDetailResponse getApplication(Long memberId, Long recruitmentId, Long applicationId) {
        throw unsupported();
    }

    public RecruitmentApplicationDetailResponse updateApplicationStatus(Long memberId,
                                                                        Long recruitmentId,
                                                                        Long applicationId,
                                                                        RecruitmentApplicationStatusUpdateRequest request,
                                                                        String clientIp) {
        throw unsupported();
    }

    public void cancelApplication(Long memberId, Long recruitmentId, Long applicationId, String clientIp) {
        throw unsupported();
    }

    @Transactional(readOnly = true)
    public ApplicationChatMessagePageResponse getMessages(Long memberId,
                                                          Long applicationId,
                                                          ApplicationChatMessageSearchRequest request) {
        throw unsupported();
    }

    public ApplicationChatMessageResponse createMessage(Long memberId,
                                                        Long applicationId,
                                                        ApplicationChatMessageCreateRequest request,
                                                        String clientIp) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(
                "Recruitment application API contract only. Service implementation is pending."
        );
    }
}
