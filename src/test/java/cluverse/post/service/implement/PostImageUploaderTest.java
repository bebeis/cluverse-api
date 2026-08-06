package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.client.PostImageStorageClient;
import cluverse.post.domain.UploadedPostImage;
import cluverse.post.properties.PostImageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostImageUploaderTest {

    @Mock
    private PostImageStorageClient postImageStorageClient;
    @Mock
    private PostImageProperties properties;

    private PostImageUploader postImageUploader;

    @BeforeEach
    void setUp() {
        when(properties.maxFileSize()).thenReturn(DataSize.ofMegabytes(10));
        postImageUploader = new PostImageUploader(postImageStorageClient, properties);
    }

    @Test
    void 파일_시그니처로_형식을_검증하고_업로드한_URL을_반환한다() {
        MockMultipartFile image = png("image.txt");
        when(postImageStorageClient.createImageUrl(anyString()))
                .thenAnswer(invocation -> "https://cdn.example.com/" + invocation.getArgument(0));

        List<UploadedPostImage> result = postImageUploader.upload(1L, List.of(image));

        assertThat(result).singleElement().satisfies(uploaded -> {
            assertThat(uploaded.fileKey()).startsWith("posts/1/").endsWith(".png");
            assertThat(uploaded.imageUrl()).isEqualTo("https://cdn.example.com/" + uploaded.fileKey());
        });
        verify(postImageStorageClient).upload(
                eq(result.getFirst().fileKey()),
                eq("image/png"),
                argThat(path -> !Files.exists(path))
        );
    }

    @Test
    void 일부_파일_업로드가_실패하면_앞서_저장한_파일을_정리한다() {
        doNothing()
                .doThrow(new IllegalStateException("storage failure"))
                .when(postImageStorageClient).upload(anyString(), anyString(), any(Path.class));
        when(postImageStorageClient.createImageUrl(anyString())).thenReturn("https://cdn.example.com/image.png");

        assertThatThrownBy(() -> postImageUploader.upload(1L, List.of(png("first.png"), png("second.png"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("storage failure");

        verify(postImageStorageClient).delete(argThat(fileKeys -> fileKeys.size() == 1));
    }

    @Test
    void 이미지가_아닌_파일은_저장하지_않는다() {
        MockMultipartFile text = new MockMultipartFile(
                "images", "memo.txt", "text/plain", "not-image".getBytes());

        assertThatThrownBy(() -> postImageUploader.upload(1L, List.of(text)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("지원하지 않거나 손상된 이미지 형식입니다.");

        verify(postImageStorageClient, never()).upload(anyString(), anyString(), any(Path.class));
    }

    private MockMultipartFile png(String originalFileName) {
        return new MockMultipartFile(
                "images",
                originalFileName,
                "application/octet-stream",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}
        );
    }
}
