package cluverse.member.service;

import cluverse.common.exception.BadRequestException;
import cluverse.member.service.request.MemberProfileImagePresignedUrlRequest;
import cluverse.member.service.response.MemberProfileImagePresignedUrlResponse;
import cluverse.post.client.PostImageStorageClient;
import cluverse.post.client.PresignedUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileImageService {

    private static final int PRESIGNED_URL_EXPIRE_MINUTES = 10;
    private static final Map<String, String> CONTENT_TYPE_EXTENSION_MAP = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final PostImageStorageClient postImageStorageClient;

    public MemberProfileImagePresignedUrlResponse createPresignedUrl(Long memberId,
                                                                     MemberProfileImagePresignedUrlRequest request) {
        String fileKey = createFileKey(memberId, request.contentType());
        PresignedUploadResult presignedUpload = postImageStorageClient.createPresignedUpload(fileKey, request.contentType());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PRESIGNED_URL_EXPIRE_MINUTES);

        return new MemberProfileImagePresignedUrlResponse(
                fileKey,
                presignedUpload.uploadUrl(),
                presignedUpload.imageUrl(),
                expiresAt
        );
    }

    private String createFileKey(Long memberId, String contentType) {
        String extension = CONTENT_TYPE_EXTENSION_MAP.get(contentType);
        if (extension == null) {
            throw new BadRequestException("지원하지 않는 이미지 형식입니다.");
        }

        LocalDate today = LocalDate.now();
        return "members/%d/profile/%d/%02d/%02d/%s.%s".formatted(
                memberId,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }
}
