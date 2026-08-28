package cluverse.post.service.implement;

import cluverse.post.domain.ImageUploadVersion;
import cluverse.post.service.request.PostImageUploadRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostImageUploadPreparerTest {

    @Test
    void 다음_이미지_준비가_실패하면_앞서_만든_임시_파일을_정리한다() {
        PostImageFileInspector inspector = mock(PostImageFileInspector.class);
        PostImageTemporaryFileStore temporaryFiles = mock(PostImageTemporaryFileStore.class);
        MockMultipartFile first = image("first");
        MockMultipartFile second = image("second");
        TemporaryPostImageFile firstTemporary = mock(TemporaryPostImageFile.class);
        when(inspector.inspect(first)).thenReturn(
                new PostImageFileInspector.PostImageSource("image/jpeg", first.getSize()));
        when(temporaryFiles.copy(first)).thenReturn(firstTemporary);
        when(inspector.inspect(second)).thenThrow(new IllegalStateException("invalid second image"));
        PostImageUploadPreparer preparer = new PostImageUploadPreparer(inspector, temporaryFiles);
        PostImageUploadRequest request = new PostImageUploadRequest(
                UUID.randomUUID(), List.of(first, second));

        assertThatThrownBy(() -> preparer.prepare(ImageUploadVersion.V3, request))
                .isInstanceOf(IllegalStateException.class);

        verify(firstTemporary).close();
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(
                "images", name + ".jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
    }
}
