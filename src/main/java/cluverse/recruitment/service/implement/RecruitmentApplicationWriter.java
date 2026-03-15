package cluverse.recruitment.service.implement;

import cluverse.recruitment.domain.FormItemAnswer;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.repository.RecruitmentApplicationRepository;
import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationAnswerRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class RecruitmentApplicationWriter {

    private final RecruitmentApplicationRepository recruitmentApplicationRepository;

    public RecruitmentApplication create(Recruitment recruitment,
                                         Long applicantId,
                                         RecruitmentApplicationCreateRequest request,
                                         String clientIp) {
        RecruitmentApplication application = RecruitmentApplication.create(
                recruitment.getId(),
                applicantId,
                request.position(),
                request.portfolioUrl(),
                toAnswers(request.answers()),
                clientIp
        );
        recruitment.increaseApplicationCount();
        return recruitmentApplicationRepository.save(application);
    }

    public void updateStatus(RecruitmentApplication application,
                             RecruitmentApplicationStatusUpdateRequest request,
                             Long actorId,
                             String clientIp) {
        application.changeStatus(request.status(), actorId, request.note(), clientIp);
    }

    public void cancel(RecruitmentApplication application, Long actorId, String clientIp) {
        application.changeStatus(RecruitmentApplicationStatus.CANCELLED, actorId, null, clientIp);
    }

    public void createMessage(RecruitmentApplication application,
                              Long senderId,
                              ApplicationChatMessageCreateRequest request,
                              String clientIp) {
        application.addMessage(senderId, request.content(), clientIp);
    }

    private List<FormItemAnswer> toAnswers(List<RecruitmentApplicationAnswerRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(request -> FormItemAnswer.create(request.formItemId(), request.answer()))
                .toList();
    }
}
