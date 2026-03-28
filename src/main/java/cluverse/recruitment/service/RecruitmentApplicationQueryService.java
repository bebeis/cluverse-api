package cluverse.recruitment.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.common.exception.NotFoundException;
import cluverse.group.domain.Group;
import cluverse.group.service.implement.GroupReader;
import cluverse.member.service.implement.MemberReader;
import cluverse.recruitment.domain.FormItem;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentApplicationQueryRepository;
import cluverse.recruitment.repository.dto.ApplicationChatMessageQueryDto;
import cluverse.recruitment.repository.dto.RecruitmentApplicationSummaryQueryDto;
import cluverse.recruitment.service.implement.RecruitmentApplicationReader;
import cluverse.recruitment.service.request.ApplicationChatMessageSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationSearchRequest;
import cluverse.recruitment.service.response.ApplicationChatMessagePageResponse;
import cluverse.recruitment.service.response.ApplicationChatMessageResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationAnswerResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationDetailResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationPageResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentApplicationQueryService {

    private final RecruitmentApplicationReader recruitmentApplicationReader;
    private final RecruitmentApplicationQueryRepository recruitmentApplicationQueryRepository;
    private final GroupReader groupReader;
    private final MemberReader memberReader;

    public RecruitmentApplicationPageResponse getMyApplications(Long memberId,
                                                               RecruitmentApplicationSearchRequest request) {
        List<RecruitmentApplicationSummaryQueryDto> queriedApplications =
                recruitmentApplicationQueryRepository.findMyApplicationSummaries(
                        memberId,
                        request.status(),
                        request.pageOrDefault(),
                        request.sizeOrDefault()
                );
        List<RecruitmentApplicationSummaryResponse> pageItems = trimToPage(
                queriedApplications, request.sizeOrDefault()
        ).stream()
                .map(this::toSummaryResponse)
                .toList();
        return new RecruitmentApplicationPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queriedApplications.size() > request.sizeOrDefault()
        );
    }

    public RecruitmentApplicationPageResponse getApplications(Long memberId,
                                                              Long recruitmentId,
                                                              RecruitmentApplicationSearchRequest request) {
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(recruitmentId);
        validateManager(memberId, recruitment.getGroupId());
        List<RecruitmentApplicationSummaryQueryDto> queriedApplications =
                recruitmentApplicationQueryRepository.findRecruitmentApplicationSummaries(
                        recruitmentId,
                        request.status(),
                        request.pageOrDefault(),
                        request.sizeOrDefault()
                );
        List<RecruitmentApplicationSummaryResponse> pageItems = trimToPage(
                queriedApplications, request.sizeOrDefault()
        ).stream()
                .map(this::toSummaryResponse)
                .toList();
        return new RecruitmentApplicationPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                queriedApplications.size() > request.sizeOrDefault()
        );
    }

    public RecruitmentApplicationDetailResponse getApplication(Long memberId, Long applicationId) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateParticipantOrManager(memberId, application);
        return toDetailResponse(application);
    }

    public ApplicationChatMessagePageResponse getMessages(Long memberId,
                                                          Long applicationId,
                                                          ApplicationChatMessageSearchRequest request) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateParticipantOrManager(memberId, application);
        List<ApplicationChatMessageQueryDto> queriedMessages =
                recruitmentApplicationQueryRepository.findApplicationMessages(
                        applicationId,
                        request.beforeMessageId(),
                        request.limitOrDefault()
                );
        List<ApplicationChatMessageResponse> pageItems = trimToPage(
                queriedMessages, request.limitOrDefault()
        ).stream()
                .map(message -> toMessageResponse(memberId, message))
                .toList();
        return new ApplicationChatMessagePageResponse(
                pageItems,
                request.beforeMessageId(),
                request.limitOrDefault(),
                queriedMessages.size() > request.limitOrDefault()
        );
    }

    public ApplicationChatMessageResponse getMessage(Long memberId, Long applicationId, Long messageId) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateParticipantOrManager(memberId, application);
        ApplicationChatMessageQueryDto message =
                recruitmentApplicationQueryRepository.findApplicationMessage(applicationId, messageId);
        if (message == null) {
            throw new NotFoundException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_NOT_FOUND.getMessage());
        }
        return toMessageResponse(memberId, message);
    }

    private RecruitmentApplicationSummaryResponse toSummaryResponse(RecruitmentApplicationSummaryQueryDto application) {
        return new RecruitmentApplicationSummaryResponse(
                application.applicationId(),
                application.recruitmentId(),
                application.groupId(),
                application.recruitmentTitle(),
                application.applicantId(),
                application.applicantNickname(),
                application.applicantProfileImageUrl(),
                application.position(),
                application.portfolioUrl(),
                application.status(),
                application.createdAt(),
                application.reviewedAt()
        );
    }

    private RecruitmentApplicationDetailResponse toDetailResponse(RecruitmentApplication application) {
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(application.getRecruitmentId());
        Map<Long, cluverse.member.domain.Member> memberMap = memberReader.readMemberMap(
                java.util.stream.Stream.of(application.getApplicantId(), application.getReviewedBy())
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList()
        );
        cluverse.member.domain.Member applicant = memberMap.get(application.getApplicantId());
        cluverse.member.domain.Member reviewer = memberMap.get(application.getReviewedBy());
        cluverse.member.domain.MemberProfile profile = applicant == null ? null : applicant.getProfile();

        return new RecruitmentApplicationDetailResponse(
                application.getId(),
                application.getRecruitmentId(),
                recruitment == null ? null : recruitment.getGroupId(),
                recruitment == null ? null : recruitment.getTitle(),
                application.getApplicantId(),
                applicant == null ? null : applicant.getNickname(),
                profile == null ? null : profile.getProfileImageUrl(),
                application.getPosition(),
                application.getPortfolioUrl(),
                application.getStatus(),
                application.getReviewedBy(),
                reviewer == null ? null : reviewer.getNickname(),
                application.getReviewedAt(),
                application.getLatestReviewNote(),
                toAnswerResponses(recruitment, application),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private List<RecruitmentApplicationAnswerResponse> toAnswerResponses(Recruitment recruitment,
                                                                         RecruitmentApplication application) {
        Map<Long, String> questionMap = recruitment == null
                ? Map.of()
                : recruitment.getFormItems().stream()
                .collect(java.util.stream.Collectors.toMap(FormItem::getId, FormItem::getQuestion));

        return application.getAnswers().stream()
                .map(answer -> RecruitmentApplicationAnswerResponse.of(
                        answer,
                        questionMap.get(answer.getFormItemId())
                ))
                .toList();
    }

    private ApplicationChatMessageResponse toMessageResponse(Long memberId,
                                                             ApplicationChatMessageQueryDto message) {
        return new ApplicationChatMessageResponse(
                message.applicationChatMessageId(),
                message.applicationId(),
                message.senderId(),
                message.senderNickname(),
                message.senderProfileImageUrl(),
                message.content(),
                memberId != null && memberId.equals(message.senderId()),
                message.isRead(),
                message.createdAt()
        );
    }

    private <T> List<T> trimToPage(List<T> items, int size) {
        return items.size() > size ? items.subList(0, size) : items;
    }

    private void validateManager(Long memberId, Long groupId) {
        Group group = groupReader.readOrThrow(groupId);
        if (!group.isManager(memberId)) {
            throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_ACCESS_DENIED.getMessage());
        }
    }

    private void validateParticipantOrManager(Long memberId, RecruitmentApplication application) {
        if (application.getApplicantId().equals(memberId)) {
            return;
        }
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(application.getRecruitmentId());
        Group group = groupReader.readOrThrow(recruitment.getGroupId());
        if (group.isManager(memberId)) {
            return;
        }
        throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_ACCESS_DENIED.getMessage());
    }
}
