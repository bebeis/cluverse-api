package cluverse.post.service.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record PostImageMultipartUploadRequest(
        @NotEmpty(message = "이미지를 한 개 이상 첨부해주세요.")
        @Size(max = 5, message = "이미지는 최대 5개까지 업로드할 수 있습니다.")
        List<MultipartFile> images
) {
    public PostImageMultipartUploadRequest {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
