package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.client.PostImageProcessCommand;
import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.domain.PostImageAsset;
import cluverse.post.properties.PostImageUploadProperties;
import cluverse.post.service.request.PostImageUploadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageUploadPreparer {

    private static final String POLICY_VERSION = "p1";

    private final PostImageUploadProperties properties;
    private final PostImageUploadTemporaryFileCleaner temporaryFileCleaner;

    public PreparedPostImageUpload prepare(ImageUploadVersion version, PostImageUploadRequest request) {
        List<PreparedPostImage> preparedImages = new ArrayList<>();
        List<PostImageAsset> assets = new ArrayList<>();
        try {
            for (int index = 0; index < request.images().size(); index++) {
                MultipartFile file = request.images().get(index);
                validateSize(file);
                String contentType = detectContentType(file);
                Path temporaryFile = Files.createTempFile("cluverse-image-upload-", ".source");
                try {
                    // File overload delegates to Servlet Part.write, allowing a same-filesystem move.
                    // MultipartFile's Path overload copies the full stream into this temporary file.
                    file.transferTo(temporaryFile.toFile());
                } catch (IOException | RuntimeException exception) {
                    temporaryFileCleaner.delete(temporaryFile);
                    throw exception;
                }

                String prefix = "image-uploads/%s/%s/%d".formatted(
                        version.value(), request.requestId(), index);
                String stagingKey = prefix + "/staging/source";
                String contentKey = prefix + "/" + POLICY_VERSION + "/content.jpg";
                String thumbnailKey = prefix + "/" + POLICY_VERSION + "/thumbnail.jpg";
                PostImageProcessCommand command = new PostImageProcessCommand(
                        request.requestId(), index, stagingKey, contentKey, thumbnailKey, POLICY_VERSION);
                preparedImages.add(new PreparedPostImage(temporaryFile, contentType, file.getSize(), command));
                assets.add(PostImageAsset.plan(
                        index, stagingKey, contentKey, thumbnailKey, file.getSize()));
            }
            return new PreparedPostImageUpload(
                    List.copyOf(preparedImages),
                    List.copyOf(assets),
                    temporaryFileCleaner
            );
        } catch (IOException exception) {
            deletePreparedFiles(preparedImages);
            throw new UncheckedIOException("업로드 임시 파일을 준비하지 못했습니다.", exception);
        } catch (RuntimeException exception) {
            deletePreparedFiles(preparedImages);
            throw exception;
        }
    }

    private void validateSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("빈 이미지 파일은 업로드할 수 없습니다.");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new BadRequestException("이미지 한 개의 최대 크기를 초과했습니다.");
        }
    }

    private String detectContentType(MultipartFile file) throws IOException {
        byte[] signature = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(signature);
        }
        if (length >= 3
                && (signature[0] & 0xFF) == 0xFF
                && (signature[1] & 0xFF) == 0xD8
                && (signature[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (length >= 8
                && signature[0] == (byte) 0x89
                && signature[1] == 'P'
                && signature[2] == 'N'
                && signature[3] == 'G') {
            return "image/png";
        }
        if (length >= 12
                && signature[0] == 'R'
                && signature[1] == 'I'
                && signature[2] == 'F'
                && signature[3] == 'F'
                && signature[8] == 'W'
                && signature[9] == 'E'
                && signature[10] == 'B'
                && signature[11] == 'P') {
            return "image/webp";
        }
        throw new BadRequestException("지원하지 않거나 손상된 이미지 형식입니다.");
    }

    private void deletePreparedFiles(List<PreparedPostImage> preparedImages) {
        for (PreparedPostImage preparedImage : preparedImages) {
            temporaryFileCleaner.delete(preparedImage.path());
        }
    }
}
