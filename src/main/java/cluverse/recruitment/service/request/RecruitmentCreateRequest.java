package cluverse.recruitment.service.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentCreateRequest(
        @NotBlank(message = "모집글 제목을 입력해주세요.")
        @Size(max = 200, message = "모집글 제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "모집글 소개를 입력해주세요.")
        String description,

        @Size(max = 20, message = "모집 포지션은 최대 20개까지 입력할 수 있습니다.")
        List<@Valid RecruitmentPositionRequest> positions,

        String requirements,
        String duration,
        String goal,
        String processDescription,
        LocalDateTime deadline,

        @Size(max = 5, message = "지원서 질문은 최대 5개까지 입력할 수 있습니다.")
        List<@Valid RecruitmentFormItemRequest> formItems
) {
    public RecruitmentCreateRequest {
        positions = positions == null ? List.of() : List.copyOf(positions);
        formItems = formItems == null ? List.of() : List.copyOf(formItems);
    }
}
