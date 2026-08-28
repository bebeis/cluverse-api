package cluverse.post.service.implement;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostImageTemporaryFileStoreTest {

    @Test
    void multipart_파일을_임시_파일로_옮기고_수명이_끝나면_정리한다() throws IOException {
        PostImageUploadTemporaryFileCleaner cleaner = mock(PostImageUploadTemporaryFileCleaner.class);
        PostImageTemporaryFileStore store = new PostImageTemporaryFileStore(cleaner);
        MockMultipartFile source = new MockMultipartFile(
                "images", "sample.jpg", "image/jpeg", new byte[]{1, 2, 3});

        TemporaryPostImageFile temporaryFile = store.copy(source);

        assertThat(Files.readAllBytes(temporaryFile.path())).containsExactly(1, 2, 3);
        temporaryFile.close();
        verify(cleaner).delete(temporaryFile.path());
        Files.deleteIfExists(temporaryFile.path());
    }

    @Test
    void multipart_이동에_실패하면_생성한_임시_파일을_정리한다() throws IOException {
        PostImageUploadTemporaryFileCleaner cleaner = mock(PostImageUploadTemporaryFileCleaner.class);
        PostImageTemporaryFileStore store = new PostImageTemporaryFileStore(cleaner);
        MultipartFile source = mock(MultipartFile.class);
        doThrow(new IOException("copy failed")).when(source).transferTo(any(File.class));

        assertThatThrownBy(() -> store.copy(source))
                .isInstanceOf(java.io.UncheckedIOException.class);

        verify(cleaner).delete(any());
    }
}
