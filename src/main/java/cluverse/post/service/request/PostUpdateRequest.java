package cluverse.post.service.request;

import cluverse.post.domain.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "본문을 입력해주세요.")
        String content,

        @NotNull(message = "카테고리를 선택해주세요.")
        PostCategory category,

        @Size(max = 10, message = "태그는 최대 10개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "태그는 비어 있을 수 없습니다.")
                @Size(max = 50, message = "태그는 50자 이하여야 합니다.")
                String> tags,

        boolean isAnonymous,
        boolean isPinned,
        boolean isExternalVisible,

        @Size(max = 10, message = "이미지는 최대 10개까지 첨부할 수 있습니다.")
        List<
                @NotBlank(message = "이미지 URL은 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
                String> imageUrls
) {
    public PostUpdateRequest {
        tags = tags == null ? List.of() : List.copyOf(tags);
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
