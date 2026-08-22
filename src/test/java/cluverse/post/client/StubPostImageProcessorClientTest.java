package cluverse.post.client;

import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.properties.PostImageProcessorMode;
import cluverse.post.properties.PostImageUploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StubPostImageProcessorClientTest {

    @Test
    void 지연_후_staging_객체를_두_결과_key로_복사한다() {
        PostImageObjectStorageClient storageClient = mock(PostImageObjectStorageClient.class);
        PostImageUploadProperties properties = properties();
        StubImageProcessingDelayProfile delayProfile = new StubImageProcessingDelayProfile(
                properties.stubAverageDelay());
        AtomicReference<Duration> slept = new AtomicReference<>();
        StubPostImageProcessorClient client = new StubPostImageProcessorClient(
                storageClient, delayProfile, slept::set);
        PostImageProcessCommand command = new PostImageProcessCommand(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                0,
                "image-uploads/request/staging/0.jpg",
                "image-uploads/request/content/0.jpg",
                "image-uploads/request/thumbnail/0.jpg",
                "p1"
        );

        ProcessedPostImage result = client.process(command);

        assertThat(slept.get()).isIn(
                Duration.ofMillis(900), Duration.ofMillis(960), Duration.ofMillis(1_040));
        verify(storageClient).copy(
                command.stagingKey(), command.contentKey(), "image/jpeg");
        verify(storageClient).copy(
                command.stagingKey(), command.thumbnailKey(), "image/jpeg");
        assertThat(result.displayOrder()).isZero();
        assertThat(result.content().objectKey()).isEqualTo(command.contentKey());
        assertThat(result.thumbnail().objectKey()).isEqualTo(command.thumbnailKey());
    }

    private PostImageUploadProperties properties() {
        return new PostImageUploadProperties(
                true,
                "token",
                PostImageProcessorMode.STUB,
                "",
                "",
                Duration.ofMillis(920),
                DataSize.ofMegabytes(10),
                32,
                16,
                16,
                Duration.ofSeconds(30),
                Duration.ofMinutes(3),
                Duration.ofSeconds(30)
        );
    }
}
