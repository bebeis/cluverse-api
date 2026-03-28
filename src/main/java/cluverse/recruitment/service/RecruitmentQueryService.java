package cluverse.recruitment.service;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.service.implement.RecruitmentReader;
import cluverse.recruitment.service.request.RecruitmentSearchRequest;
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
@Transactional(readOnly = true)
public class RecruitmentQueryService {

    private final RecruitmentReader recruitmentReader;
    private final MemberReader memberReader;

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

    public RecruitmentDetailResponse getRecruitment(Long memberId, Long recruitmentId) {
        return toDetailResponse(recruitmentReader.readOrThrow(recruitmentId));
    }

    private RecruitmentDetailResponse toDetailResponse(Recruitment recruitment) {
        Map<Long, Member> memberMap = memberReader.readMemberMap(List.of(recruitment.getAuthorId()));
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
}
