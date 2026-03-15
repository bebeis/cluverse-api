package cluverse.recruitment.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.member.domain.Member;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.service.implement.RecruitmentReader;
import cluverse.recruitment.service.implement.RecruitmentWriter;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentSearchRequest;
import cluverse.recruitment.service.request.RecruitmentStatusUpdateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import cluverse.recruitment.service.response.RecruitmentFormItemResponse;
import cluverse.recruitment.service.response.RecruitmentPageResponse;
import cluverse.recruitment.service.response.RecruitmentPositionResponse;
import cluverse.recruitment.service.response.RecruitmentSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitmentService {

    private final RecruitmentReader recruitmentReader;
    private final RecruitmentWriter recruitmentWriter;

    @Transactional(readOnly = true)
    public RecruitmentPageResponse getRecruitments(Long memberId, RecruitmentSearchRequest request) {
        List<Recruitment> recruitments = recruitmentReader.readRecruitments(request);
        List<RecruitmentSummaryResponse> pageItems = paginate(recruitments, request.pageOrDefault(), request.sizeOrDefault()).stream()
                .map(RecruitmentSummaryResponse::from)
                .toList();
        return new RecruitmentPageResponse(
                pageItems,
                request.pageOrDefault(),
                request.sizeOrDefault(),
                recruitments.size() > request.pageOrDefault() * request.sizeOrDefault()
        );
    }

    public RecruitmentDetailResponse createRecruitment(Long memberId, Long groupId, RecruitmentCreateRequest request) {
        Group group = recruitmentReader.readGroupOrThrow(groupId);
        validateGroupManager(memberId, group);
        Recruitment recruitment = recruitmentWriter.create(memberId, groupId, request);
        return toDetailResponse(recruitmentReader.readOrThrow(recruitment.getId()));
    }

    @Transactional(readOnly = true)
    public RecruitmentDetailResponse getRecruitment(Long memberId, Long recruitmentId) {
        return toDetailResponse(recruitmentReader.readOrThrow(recruitmentId));
    }

    public RecruitmentDetailResponse updateRecruitment(Long memberId,
                                                       Long recruitmentId,
                                                       RecruitmentUpdateRequest request) {
        Recruitment recruitment = recruitmentReader.readOrThrow(recruitmentId);
        validateGroupManager(memberId, recruitmentReader.readGroupOrThrow(recruitment.getGroupId()));
        recruitmentWriter.update(recruitment, request);
        return toDetailResponse(recruitment);
    }

    public RecruitmentDetailResponse updateRecruitmentStatus(Long memberId,
                                                             Long recruitmentId,
                                                             RecruitmentStatusUpdateRequest request) {
        Recruitment recruitment = recruitmentReader.readOrThrow(recruitmentId);
        validateGroupManager(memberId, recruitmentReader.readGroupOrThrow(recruitment.getGroupId()));
        recruitmentWriter.updateStatus(recruitment, request);
        return toDetailResponse(recruitment);
    }

    public void deleteRecruitment(Long memberId, Long recruitmentId) {
        Recruitment recruitment = recruitmentReader.readOrThrow(recruitmentId);
        validateGroupManager(memberId, recruitmentReader.readGroupOrThrow(recruitment.getGroupId()));
        recruitmentWriter.delete(recruitment);
    }

    private RecruitmentDetailResponse toDetailResponse(Recruitment recruitment) {
        Map<Long, Member> memberMap = recruitmentReader.readMemberMap(List.of(recruitment.getAuthorId()));
        Member author = memberMap.get(recruitment.getAuthorId());
        return new RecruitmentDetailResponse(
                recruitment.getId(),
                recruitment.getGroupId(),
                recruitment.getAuthorId(),
                author == null ? null : author.getNickname(),
                recruitment.getTitle(),
                recruitment.getDescription(),
                recruitment.getPositions().stream()
                        .map(RecruitmentPositionResponse::from)
                        .toList(),
                recruitment.getRequirements(),
                recruitment.getDuration(),
                recruitment.getGoal(),
                recruitment.getProcessDescription(),
                recruitment.getDeadline(),
                recruitment.getStatus(),
                recruitment.getApplicationCount(),
                recruitment.getFormItems().stream()
                        .map(RecruitmentFormItemResponse::from)
                        .toList(),
                recruitment.getCreatedAt(),
                recruitment.getUpdatedAt()
        );
    }

    private List<Recruitment> paginate(List<Recruitment> items, int page, int size) {
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private void validateGroupManager(Long memberId, Group group) {
        if (!group.isManager(memberId)) {
            throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_ACCESS_DENIED.getMessage());
        }
    }
}
