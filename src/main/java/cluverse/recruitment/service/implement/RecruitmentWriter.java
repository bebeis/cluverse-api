package cluverse.recruitment.service.implement;

import cluverse.recruitment.domain.FormItem;
import cluverse.recruitment.domain.Recruitment;
import cluverse.recruitment.domain.RecruitmentPosition;
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

    public Recruitment create(Long memberId, Long groupId, RecruitmentCreateRequest request) {
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

    public void update(Recruitment recruitment, RecruitmentUpdateRequest request) {
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
    }

    public void updateStatus(Recruitment recruitment, RecruitmentStatusUpdateRequest request) {
        recruitment.changeStatus(request.status());
    }

    public void delete(Recruitment recruitment) {
        recruitment.delete();
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
