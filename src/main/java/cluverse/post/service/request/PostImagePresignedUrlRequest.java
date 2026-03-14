package cluverse.post.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostImagePresignedUrlRequest(
        @NotBlank(message = "원본 파일명을 입력해주세요.")
        @Size(max = 255, message = "원본 파일명은 255자 이하여야 합니다.")
        String originalFileName,

        @NotBlank(message = "콘텐츠 타입을 입력해주세요.")
        @Pattern(
                regexp = "^image/(jpeg|png|gif|webp)$",
                message = "지원하지 않는 이미지 형식입니다."
        )
        String contentType
) {
}
