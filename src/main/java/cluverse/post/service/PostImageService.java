package cluverse.post.service;

import cluverse.post.client.PostImageStorageClient;
import cluverse.post.client.PresignedUploadResult;
import cluverse.common.exception.BadRequestException;
import cluverse.post.exception.PostExceptionMessage;
import cluverse.post.service.request.PostImagePresignedUrlRequest;
import cluverse.post.service.response.PostImagePresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PostImageService {

    private static final int PRESIGNED_URL_EXPIRE_MINUTES = 10;
    private static final Map<String, String> CONTENT_TYPE_EXTENSION_MAP = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final PostImageStorageClient postImageStorageClient;

    @Transactional(readOnly = true)
    public PostImagePresignedUrlResponse createPresignedUrl(
            Long memberId,
            PostImagePresignedUrlRequest request
    ) {
        String fileKey = createFileKey(memberId, request.contentType());
        PresignedUploadResult presignedUpload = postImageStorageClient.createPresignedUpload(fileKey, request.contentType());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PRESIGNED_URL_EXPIRE_MINUTES);

        return new PostImagePresignedUrlResponse(
                fileKey,
                presignedUpload.uploadUrl(),
                presignedUpload.imageUrl(),
                expiresAt
        );
    }

    private String createFileKey(Long memberId, String contentType) {
        String extension = CONTENT_TYPE_EXTENSION_MAP.get(contentType);
        if (extension == null) {
            throw new BadRequestException(PostExceptionMessage.UNSUPPORTED_IMAGE_CONTENT_TYPE.getMessage());
        }

        LocalDate today = LocalDate.now();
        return "posts/%d/%d/%02d/%02d/%s.%s".formatted(
                memberId,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }
}
