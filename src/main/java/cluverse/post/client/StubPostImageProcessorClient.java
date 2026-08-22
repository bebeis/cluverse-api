package cluverse.post.client;

import cluverse.post.domain.PostImageMetadata;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.properties.PostImageUploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(
        prefix = "image-upload-experiment",
        name = "processor-mode",
        havingValue = "stub"
)
public class StubPostImageProcessorClient implements PostImageProcessorClient {

    private static final String CONTENT_TYPE = "image/jpeg";

    private final PostImageObjectStorageClient storageClient;
    private final StubImageProcessingDelayProfile delayProfile;
    private final Sleeper sleeper;

    @Autowired
    public StubPostImageProcessorClient(
            PostImageObjectStorageClient storageClient,
            PostImageUploadProperties properties
    ) {
        this(storageClient, new StubImageProcessingDelayProfile(properties.stubAverageDelay()),
                duration -> Thread.sleep(duration.toMillis()));
    }

    StubPostImageProcessorClient(
            PostImageObjectStorageClient storageClient,
            StubImageProcessingDelayProfile delayProfile,
            Sleeper sleeper
    ) {
        this.storageClient = storageClient;
        this.delayProfile = delayProfile;
        this.sleeper = sleeper;
    }

    @Override
    public ProcessedPostImage process(PostImageProcessCommand command) {
        sleep(delayProfile.delayFor(command));
        storageClient.copy(command.stagingKey(), command.contentKey(), CONTENT_TYPE);
        storageClient.copy(command.stagingKey(), command.thumbnailKey(), CONTENT_TYPE);
        return new ProcessedPostImage(
                command.displayOrder(),
                metadata(command.contentKey()),
                metadata(command.thumbnailKey())
        );
    }

    private PostImageMetadata metadata(String objectKey) {
        return new PostImageMetadata(objectKey, CONTENT_TYPE, 0, 0, 0);
    }

    private void sleep(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("mock 이미지 변환 대기 중 요청이 중단됐습니다.", exception);
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}
