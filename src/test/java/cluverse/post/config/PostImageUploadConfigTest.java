package cluverse.post.config;

import cluverse.post.properties.PostImageProcessorMode;
import cluverse.post.properties.PostImageUploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class PostImageUploadConfigTest {

    @Test
    void platform_worker와_virtual_task_limit을_독립적으로_설정한다() {
        PostImageUploadProperties properties = new PostImageUploadProperties(
                true,
                "token",
                PostImageProcessorMode.STUB,
                "",
                "",
                Duration.ofMillis(920),
                DataSize.ofMegabytes(10),
                256,
                64,
                128,
                Duration.ofSeconds(30),
                Duration.ofMinutes(3),
                Duration.ofSeconds(30)
        );
        PostImageUploadConfig config = new PostImageUploadConfig();

        ThreadPoolExecutor platformExecutor = config.postImagePlatformExecutor(properties);
        Semaphore virtualTaskSemaphore = config.postImageRemoteCallSemaphore(properties);

        assertThat(platformExecutor.getMaximumPoolSize()).isEqualTo(64);
        assertThat(platformExecutor.getQueue().remainingCapacity()).isEqualTo(256);
        assertThat(virtualTaskSemaphore.availablePermits()).isEqualTo(128);
        platformExecutor.shutdown();
    }
}
