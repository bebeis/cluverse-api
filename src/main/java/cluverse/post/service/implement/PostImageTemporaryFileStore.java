package cluverse.post.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PostImageTemporaryFileStore {

    private final PostImageUploadTemporaryFileCleaner cleaner;

    public TemporaryPostImageFile copy(MultipartFile source) {
        Path path = null;
        try {
            path = Files.createTempFile("cluverse-image-upload-", ".source");
            // File 기반 transfer를 사용해 같은 파일시스템에서는 추가 복사 없이 이동할 수 있게 한다.
            source.transferTo(path.toFile());
            return new TemporaryPostImageFile(path, cleaner);
        } catch (IOException failure) {
            delete(path);
            throw new UncheckedIOException("업로드 임시 파일을 준비하지 못했습니다.", failure);
        } catch (RuntimeException failure) {
            delete(path);
            throw failure;
        }
    }

    private void delete(Path path) {
        if (path != null) {
            cleaner.delete(path);
        }
    }
}
