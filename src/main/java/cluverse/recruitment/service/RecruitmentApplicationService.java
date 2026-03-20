package cluverse.recruitment.service;

import cluverse.common.exception.BadRequestException;
import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.service.GroupService;
import cluverse.member.domain.Member;
import cluverse.member.domain.MemberProfile;
import cluverse.member.service.MemberService;
import cluverse.recruitment.domain.ApplicationChatMessage;
import cluverse.recruitment.domain.FormItem;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentApplication;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentApplicationService {

    private final RecruitmentApplicationReader recruitmentApplicationReader;
    private final RecruitmentApplicationWriter recruitmentApplicationWriter;
    private final GroupService groupService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public RecruitmentApplicationPageResponse getMyApplications(Long memberId,
                                                               RecruitmentApplicationSearchRequest request) {
        List<RecruitmentApplication> applications = recruitmentApplicationReader.readMyApplications(memberId, request);
        List<RecruitmentApplication> pagedApplications = paginate(
                applications, request.pageOrDefault(), request.sizeOrDefault()
        );
        List<RecruitmentApplicationSummaryResponse> pageItems = toSummaryResponses(pagedApplications);
        return new RecruitmentApplicationPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                applications.size() > request.pageOrDefault() * request.sizeOrDefault()
        );
    }

    @Transactional(readOnly = true)
    public RecruitmentApplicationPageResponse getApplications(Long memberId,
                                                              Long recruitmentId,
                                                              RecruitmentApplicationSearchRequest request) {
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(recruitmentId);
        validateManager(memberId, recruitment.getGroupId());
        List<RecruitmentApplication> applications = recruitmentApplicationReader.readApplications(recruitmentId, request);
        List<RecruitmentApplication> pagedApplications = paginate(
                applications, request.pageOrDefault(), request.sizeOrDefault()
        );
        List<RecruitmentApplicationSummaryResponse> pageItems = toSummaryResponses(pagedApplications);
        return new RecruitmentApplicationPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                applications.size() > request.pageOrDefault() * request.sizeOrDefault()
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
        List<ApplicationChatMessage> filteredMessages = filterMessages(application.getMessages(), request.beforeMessageId());
        List<ApplicationChatMessage> pageMessages = paginateMessages(filteredMessages, request.limitOrDefault());
        Map<Long, Member> senderMap = memberService.readMemberMap(pageMessages.stream()
                .map(ApplicationChatMessage::getSenderId)
                .distinct()
                .toList());
        List<ApplicationChatMessageResponse> pageItems = pageMessages.stream()
                .map(message -> toMessageResponse(memberId, message, senderMap))
                .toList();
        return new ApplicationChatMessagePageResponse(
                pageItems,
                request.beforeMessageId(),
                request.limitOrDefault(),
                filteredMessages.size() > pageItems.size()
        );
    }

    public ApplicationChatMessageResponse createMessage(Long memberId,
                                                        Long applicationId,
                                                        ApplicationChatMessageCreateRequest request,
                                                        String clientIp) {
        RecruitmentApplication application = recruitmentApplicationReader.readOrThrow(applicationId);
        validateParticipantOrManager(memberId, application);
        recruitmentApplicationWriter.createMessage(application, memberId, request, clientIp);
        return toMessageResponse(
                memberId,
                application.getMessages().getLast(),
                memberService.readMemberMap(List.of(memberId))
        );
    }

    private List<RecruitmentApplicationSummaryResponse> toSummaryResponses(List<RecruitmentApplication> applications) {
        if (applications.isEmpty()) {
            return List.of();
        }

        Map<Long, Recruitment> recruitmentMap = recruitmentApplicationReader.readRecruitmentMap(applications.stream()
                .map(RecruitmentApplication::getRecruitmentId)
                .distinct()
                .toList());
        Map<Long, Member> memberMap = memberService.readMemberMap(applications.stream()
                .map(RecruitmentApplication::getApplicantId)
                .distinct()
                .toList());

        return applications.stream()
                .map(application -> toSummaryResponse(application, recruitmentMap, memberMap))
                .toList();
    }

    private RecruitmentApplicationSummaryResponse toSummaryResponse(RecruitmentApplication application,
                                                                    Map<Long, Recruitment> recruitmentMap,
                                                                    Map<Long, Member> memberMap) {
        Recruitment recruitment = recruitmentMap.get(application.getRecruitmentId());
        Member applicant = memberMap.get(application.getApplicantId());
        MemberProfile profile = applicant == null ? null : applicant.getProfile();

        return new RecruitmentApplicationSummaryResponse(
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
                application.getCreatedAt(),
                application.getReviewedAt()
        );
    }

    private RecruitmentApplicationDetailResponse toDetailResponse(RecruitmentApplication application) {
        Recruitment recruitment = recruitmentApplicationReader.readRecruitmentOrThrow(application.getRecruitmentId());
        Map<Long, Member> memberMap = memberService.readMemberMap(
                java.util.stream.Stream.of(application.getApplicantId(), application.getReviewedBy())
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );
        Member applicant = memberMap.get(application.getApplicantId());
        Member reviewer = memberMap.get(application.getReviewedBy());
        MemberProfile profile = applicant == null ? null : applicant.getProfile();

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
                                                             ApplicationChatMessage message,
                                                             Map<Long, Member> senderMap) {
        Member sender = senderMap.get(message.getSenderId());
        MemberProfile profile = sender == null ? null : sender.getProfile();
        return new ApplicationChatMessageResponse(
                message.getId(),
                message.getApplication().getId(),
                message.getSenderId(),
                sender == null ? null : sender.getNickname(),
                profile == null ? null : profile.getProfileImageUrl(),
                message.getContent(),
                memberId != null && memberId.equals(message.getSenderId()),
                message.isRead(),
                message.getCreatedAt()
        );
    }

    private List<RecruitmentApplication> paginate(List<RecruitmentApplication> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private List<ApplicationChatMessage> filterMessages(List<ApplicationChatMessage> messages, Long beforeMessageId) {
        if (beforeMessageId == null) {
            return messages;
        }
        return messages.stream()
                .filter(message -> message.getId() < beforeMessageId)
                .toList();
    }

    private List<ApplicationChatMessage> paginateMessages(List<ApplicationChatMessage> messages, int limit) {
        int toIndex = Math.min(limit, messages.size());
        return messages.subList(0, toIndex);
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
