package cluverse.recruitment.service;

import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.service.GroupService;
import cluverse.member.service.MemberService;
import cluverse.recruitment.domain.FormItem;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentApplicationQueryRepository;
import cluverse.recruitment.repository.dto.ApplicationChatMessageQueryDto;
import cluverse.recruitment.repository.dto.RecruitmentApplicationSummaryQueryDto;
import cluverse.recruitment.service.implement.RecruitmentApplicationReader;
import cluverse.recruitment.service.implement.RecruitmentApplicationWriter;
import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.ApplicationChatMessageSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationSearchRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
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
@Transactional
public class RecruitmentApplicationService {

    private final RecruitmentApplicationReader recruitmentApplicationReader;
    private final RecruitmentApplicationWriter recruitmentApplicationWriter;
    private final RecruitmentApplicationQueryRepository recruitmentApplicationQueryRepository;
    private final GroupService groupService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    public RecruitmentApplicationDetailResponse createApplication(Long memberId,
                                                                  Long recruitmentId,
                                                                  RecruitmentApplicationCreateRequest request,
                                                                  String clientIp) {
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(recruitmentId);
        Group group = groupService.readGroupOrThrow(recruitment.getGroupId());
        validateApplicationCreatable(memberId, recruitmentId, recruitment, group);
        RecruitmentApplication application = recruitmentApplicationWriter.create(recruitment, memberId, request, clientIp);
        return toDetailResponse(application);
    }

    @Transactional(readOnly = true)
    public RecruitmentApplicationDetailResponse getApplication(Long memberId, Long applicationId) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateParticipantOrManager(memberId, application);
        return toDetailResponse(application);
    }

    public RecruitmentApplicationDetailResponse updateApplicationStatus(Long memberId,
                                                                        Long applicationId,
                                                                        RecruitmentApplicationStatusUpdateRequest request,
                                                                        String clientIp) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(application.getRecruitmentId());
        validateManager(memberId, recruitment.getGroupId());
        validateApplicationStatusChangeable(application);
        recruitmentApplicationWriter.updateStatus(application, request, memberId, clientIp);
        addMemberIfApproved(request, recruitment, application);
        return toDetailResponse(application);
    }

    public void cancelApplication(Long memberId, Long applicationId, String clientIp) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateApplicant(memberId, application);
        recruitmentApplicationWriter.cancel(application, memberId, clientIp);
    }

    @Transactional(readOnly = true)
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

    public ApplicationChatMessageResponse createMessage(Long memberId,
                                                        Long applicationId,
                                                        ApplicationChatMessageCreateRequest request,
                                                        String clientIp) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateParticipantOrManager(memberId, application);
        recruitmentApplicationWriter.createMessage(application, memberId, request, clientIp);
        ApplicationChatMessageQueryDto message =
                recruitmentApplicationQueryRepository.findApplicationMessages(applicationId, null, 1).getFirst();
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
        Map<Long, cluverse.member.domain.Member> memberMap = memberService.readMemberMap(
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
        Group group = groupService.readGroupOrThrow(groupId);
        if (!group.isManager(memberId)) {
            throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_ACCESS_DENIED.getMessage());
        }
    }

    private void validateParticipantOrManager(Long memberId, RecruitmentApplication application) {
        if (application.getApplicantId().equals(memberId)) {
            return;
        }
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(application.getRecruitmentId());
        Group group = groupService.readGroupOrThrow(recruitment.getGroupId());
        if (group.isManager(memberId)) {
            return;
        }
        throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_ACCESS_DENIED.getMessage());
    }

    private void validateApplicationCreatable(Long memberId,
                                              Long recruitmentId,
                                              Recruitment recruitment,
                                              Group group) {
        validateRecruitmentOpen(recruitment);
        validateDuplicateApplication(memberId, recruitmentId);
        validateGroupMemberApplicant(memberId, group);
    }

    private void validateRecruitmentOpen(Recruitment recruitment) {
        if (!recruitment.isOpen()) {
            throw new BadRequestException(RecruitmentExceptionMessage.RECRUITMENT_NOT_OPEN.getMessage());
        }
    }

    private void validateDuplicateApplication(Long memberId, Long recruitmentId) {
        if (recruitmentApplicationReader.existsByRecruitmentAndApplicant(recruitmentId, memberId)) {
            throw new BadRequestException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateGroupMemberApplicant(Long memberId, Group group) {
        if (group.hasMember(memberId)) {
            throw new BadRequestException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_ALREADY_EXISTS.getMessage());
        }
    }

    private void validateApplicationStatusChangeable(RecruitmentApplication application) {
        if (application.getStatus() == RecruitmentApplicationStatus.CANCELLED) {
            throw new BadRequestException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_STATUS_INVALID.getMessage());
        }
    }

    private void addMemberIfApproved(RecruitmentApplicationStatusUpdateRequest request,
                                     Recruitment recruitment,
                                     RecruitmentApplication application) {
        if (request.status() != RecruitmentApplicationStatus.APPROVED) {
            return;
        }

        Group group = groupService.readGroupOrThrow(recruitment.getGroupId());
        if (!group.hasMember(application.getApplicantId())) {
            group.addMember(application.getApplicantId());
        }
    }

    private void validateApplicant(Long memberId, RecruitmentApplication application) {
        if (!application.getApplicantId().equals(memberId)) {
            throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_APPLICATION_ACCESS_DENIED.getMessage());
        }
    }
}
