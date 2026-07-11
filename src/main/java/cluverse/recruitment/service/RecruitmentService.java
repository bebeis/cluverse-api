package cluverse.recruitment.service;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.service.implement.RecruitmentReader;
import cluverse.recruitment.service.implement.RecruitmentWriter;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentStatusUpdateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import cluverse.recruitment.service.response.RecruitmentFormItemResponse;
import cluverse.recruitment.service.response.RecruitmentPositionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final RecruitmentReader recruitmentReader;
    private final RecruitmentWriter recruitmentWriter;
    private final MemberReader memberReader;

    public RecruitmentDetailResponse createRecruitment(Long memberId, Long groupId, RecruitmentCreateRequest request) {
        Recruitment recruitment = recruitmentWriter.create(memberId, groupId, request);
        return toDetailResponse(recruitmentReader.readOrThrow(recruitment.getId()));
    }

    public RecruitmentDetailResponse updateRecruitment(Long memberId,
                                                       Long recruitmentId,
                                                       RecruitmentUpdateRequest request) {
        return toDetailResponse(recruitmentWriter.update(memberId, recruitmentId, request));
    }

    public RecruitmentDetailResponse updateRecruitmentStatus(Long memberId,
                                                             Long recruitmentId,
                                                             RecruitmentStatusUpdateRequest request) {
        return toDetailResponse(recruitmentWriter.updateStatus(memberId, recruitmentId, request));
    }

    public void deleteRecruitment(Long memberId, Long recruitmentId) {
        recruitmentWriter.delete(memberId, recruitmentId);
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
}
