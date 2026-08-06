package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.client.PostImageStorageClient;
import cluverse.post.domain.UploadedPostImage;
import cluverse.post.properties.PostImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostImageUploader {

    private static final Map<String, String> CONTENT_TYPE_EXTENSION_MAP = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final PostImageStorageClient postImageStorageClient;
    private final PostImageProperties properties;

    public List<UploadedPostImage> upload(Long memberId, List<MultipartFile> images) {
        List<String> uploadedFileKeys = new ArrayList<>();
        List<UploadedPostImage> uploadedImages = new ArrayList<>();
        try {
            for (MultipartFile image : images) {
                validateSize(image);
                String contentType = detectContentType(image);
                String fileKey = createFileKey(memberId, contentType);
                upload(image, fileKey, contentType);
                uploadedFileKeys.add(fileKey);
                uploadedImages.add(new UploadedPostImage(
                        fileKey,
                        postImageStorageClient.createImageUrl(fileKey)
                ));
            }
            return List.copyOf(uploadedImages);
        } catch (RuntimeException exception) {
            compensate(uploadedFileKeys, exception);
            throw exception;
        }
    }

    private void upload(MultipartFile image, String fileKey, String contentType) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("cluverse-post-image-", ".source");
            image.transferTo(temporaryFile);
            postImageStorageClient.upload(fileKey, contentType, temporaryFile);
        } catch (IOException exception) {
            throw new UncheckedIOException("게시글 이미지 임시 파일을 준비하지 못했습니다.", exception);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    private void validateSize(MultipartFile image) {
        if (image.isEmpty()) {
            throw new BadRequestException("빈 이미지 파일은 업로드할 수 없습니다.");
        }
        if (image.getSize() > properties.maxFileSize().toBytes()) {
            throw new BadRequestException("이미지 한 개의 최대 크기를 초과했습니다.");
        }
    }

    private String detectContentType(MultipartFile image) {
        byte[] signature = new byte[12];
        int length;
        try (InputStream input = image.getInputStream()) {
            length = input.read(signature);
        } catch (IOException exception) {
            throw new UncheckedIOException("게시글 이미지 형식을 확인하지 못했습니다.", exception);
        }
        if (isJpeg(signature, length)) {
            return "image/jpeg";
        }
        if (isPng(signature, length)) {
            return "image/png";
        }
        if (isGif(signature, length)) {
            return "image/gif";
        }
        if (isWebp(signature, length)) {
            return "image/webp";
        }
        throw new BadRequestException("지원하지 않거나 손상된 이미지 형식입니다.");
    }

    private boolean isJpeg(byte[] signature, int length) {
        return length >= 3
                && (signature[0] & 0xFF) == 0xFF
                && (signature[1] & 0xFF) == 0xD8
                && (signature[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] signature, int length) {
        return length >= 8
                && signature[0] == (byte) 0x89
                && signature[1] == 'P'
                && signature[2] == 'N'
                && signature[3] == 'G';
    }

    private boolean isGif(byte[] signature, int length) {
        return length >= 6
                && signature[0] == 'G'
                && signature[1] == 'I'
                && signature[2] == 'F'
                && signature[3] == '8'
                && (signature[4] == '7' || signature[4] == '9')
                && signature[5] == 'a';
    }

    private boolean isWebp(byte[] signature, int length) {
        return length >= 12
                && signature[0] == 'R'
                && signature[1] == 'I'
                && signature[2] == 'F'
                && signature[3] == 'F'
                && signature[8] == 'W'
                && signature[9] == 'E'
                && signature[10] == 'B'
                && signature[11] == 'P';
    }

    private String createFileKey(Long memberId, String contentType) {
        String extension = CONTENT_TYPE_EXTENSION_MAP.get(contentType);
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

    private void compensate(List<String> uploadedFileKeys, RuntimeException failure) {
        if (uploadedFileKeys.isEmpty()) {
            return;
        }
        try {
            postImageStorageClient.delete(uploadedFileKeys);
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // 요청 수명 밖의 임시 파일 정리는 운영체제의 임시 디렉터리 정책에 맡긴다.
        }
    }
}
