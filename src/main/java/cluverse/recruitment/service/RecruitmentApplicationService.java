package cluverse.recruitment.service;

import cluverse.recruitment.service.implement.RecruitmentApplicationProcessor;
import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecruitmentApplicationService {

    private final RecruitmentApplicationProcessor recruitmentApplicationProcessor;

    public Long createApplication(Long memberId,
                                  Long recruitmentId,
                                  RecruitmentApplicationCreateRequest request,
                                  String clientIp) {
        return recruitmentApplicationProcessor.createApplication(memberId, recruitmentId, request, clientIp);
    }

    public Long updateApplicationStatus(Long memberId,
                                        Long applicationId,
                                        RecruitmentApplicationStatusUpdateRequest request,
                                        String clientIp) {
        return recruitmentApplicationProcessor.updateApplicationStatus(memberId, applicationId, request, clientIp);
    }

    public void cancelApplication(Long memberId, Long applicationId, String clientIp) {
        recruitmentApplicationProcessor.cancelApplication(memberId, applicationId, clientIp);
    }

    public Long createMessage(Long memberId,
                              Long applicationId,
                              ApplicationChatMessageCreateRequest request,
                              String clientIp) {
        return recruitmentApplicationProcessor.createMessage(memberId, applicationId, request, clientIp);
    }
}
