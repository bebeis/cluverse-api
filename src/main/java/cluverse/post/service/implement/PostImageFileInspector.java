package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.properties.PostImageUploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Component
@RequiredArgsConstructor
public class PostImageFileInspector {

    private final PostImageUploadProperties properties;

    public PostImageSource inspect(MultipartFile file) {
        validateSize(file);
        return new PostImageSource(detectContentType(file), file.getSize());
    }

    private void validateSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("빈 이미지 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new BadRequestException("이미지 한 개의 최대 크기를 초과했습니다.");
        }
    }

    private String detectContentType(MultipartFile file) {
        byte[] signature = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(signature);
        } catch (IOException failure) {
            throw new UncheckedIOException("게시글 이미지 형식을 확인하지 못했습니다.", failure);
        }
        if (isJpeg(signature, length)) {
            return "image/jpeg";
        }
        if (isPng(signature, length)) {
            return "image/png";
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

    public record PostImageSource(String contentType, long bytes) {
    }
}
