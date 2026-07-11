package cluverse.recruitment.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.service.implement.GroupReader;
import cluverse.recruitment.domain.FormItem;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentPosition;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import cluverse.recruitment.repository.RecruitmentRepository;
import cluverse.recruitment.service.request.RecruitmentCreateRequest;
import cluverse.recruitment.service.request.RecruitmentFormItemRequest;
import cluverse.recruitment.service.request.RecruitmentPositionRequest;
import cluverse.recruitment.service.request.RecruitmentStatusUpdateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class RecruitmentWriter {

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentReader recruitmentReader;
    private final GroupReader groupReader;

    public Recruitment create(Long memberId, Long groupId, RecruitmentCreateRequest request) {
        validateGroupManager(memberId, groupId);
        Recruitment recruitment = Recruitment.create(
                groupId,
                memberId,
                request.title(),
                request.description(),
                toPositions(request.positions()),
                request.requirements(),
                request.duration(),
                request.goal(),
                request.processDescription(),
                request.deadline(),
                toFormItems(request.formItems())
        );
        return recruitmentRepository.save(recruitment);
    }

    public Recruitment update(Long memberId, Long recruitmentId, RecruitmentUpdateRequest request) {
        Recruitment recruitment = readManagedRecruitment(memberId, recruitmentId);
        recruitment.update(
                request.title(),
                request.description(),
                toPositions(request.positions()),
                request.requirements(),
                request.duration(),
                request.goal(),
                request.processDescription(),
                request.deadline(),
                toFormItems(request.formItems())
        );
        return recruitment;
    }

    public Recruitment updateStatus(Long memberId, Long recruitmentId, RecruitmentStatusUpdateRequest request) {
        Recruitment recruitment = readManagedRecruitment(memberId, recruitmentId);
        recruitment.changeStatus(request.status());
        return recruitment;
    }

    public void delete(Long memberId, Long recruitmentId) {
        Recruitment recruitment = readManagedRecruitment(memberId, recruitmentId);
        recruitment.delete();
    }

    private Recruitment readManagedRecruitment(Long memberId, Long recruitmentId) {
        Recruitment recruitment = recruitmentReader.readOrThrow(recruitmentId);
        validateGroupManager(memberId, recruitment.getGroupId());
        return recruitment;
    }

    private void validateGroupManager(Long memberId, Long groupId) {
        Group group = groupReader.readOrThrow(groupId);
        if (!group.isManager(memberId)) {
            throw new ForbiddenException(RecruitmentExceptionMessage.RECRUITMENT_ACCESS_DENIED.getMessage());
        }
    }

    private List<RecruitmentPosition> toPositions(List<RecruitmentPositionRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(request -> new RecruitmentPosition(request.name(), request.count()))
                .toList();
    }

    private List<FormItem> toFormItems(List<RecruitmentFormItemRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(request -> FormItem.create(
                        request.question(),
                        request.questionType(),
                        request.isRequired(),
                        request.options(),
                        request.displayOrder()
                ))
                .toList();
    }
}
