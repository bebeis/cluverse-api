package cluverse.post.service.implement;

import cluverse.common.exception.BadRequestException;
import cluverse.post.properties.PostImageUploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostImageFileInspectorTest {

    private final PostImageFileInspector inspector = new PostImageFileInspector(properties());

    @Test
    void 파일_시그니처로_JPEG를_판별한다() {
        MockMultipartFile image = new MockMultipartFile(
                "images", "sample.bin", "application/octet-stream",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}
        );

        PostImageFileInspector.PostImageSource source = inspector.inspect(image);

        assertThat(source.contentType()).isEqualTo("image/jpeg");
        assertThat(source.bytes()).isEqualTo(4);
    }

    @Test
    void 지원하지_않는_시그니처는_거절한다() {
        MockMultipartFile image = new MockMultipartFile(
                "images", "sample.bin", "application/octet-stream", new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> inspector.inspect(image))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("지원하지 않거나 손상된 이미지 형식입니다.");
    }

    private PostImageUploadProperties properties() {
        return new PostImageUploadProperties(
                "image-processor", "", DataSize.ofMegabytes(10), 16,
                Duration.ofSeconds(30), Duration.ofMinutes(3),
                Duration.ofHours(24), Duration.ofSeconds(30));
    }
}
