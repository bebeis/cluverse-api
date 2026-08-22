package cluverse.post.config;

import cluverse.common.properties.AwsProperties;
import cluverse.post.properties.PostImageUploadProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(PostImageUploadProperties.class)
public class PostImageUploadConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "image-upload-experiment",
            name = "processor-mode",
            havingValue = "lambda",
            matchIfMissing = true
    )
    LambdaClient postImageLambdaClient(
            AwsProperties awsProperties,
            PostImageUploadProperties properties
    ) {
        LambdaClientBuilder builder = LambdaClient.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (StringUtils.hasText(properties.lambdaEndpoint())) {
            builder.endpointOverride(URI.create(properties.lambdaEndpoint()));
        }
        return builder.build();
    }

    @Bean(name = "postImagePlatformExecutor", destroyMethod = "shutdown")
    ThreadPoolExecutor postImagePlatformExecutor(PostImageUploadProperties properties) {
        ThreadFactory threadFactory = Thread.ofPlatform()
                .name("image-upload-platform-", 0)
                .factory();
        return new ThreadPoolExecutor(
                properties.maxConcurrentRemoteCalls(),
                properties.maxConcurrentRemoteCalls(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.platformQueueCapacity()),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "postImageVirtualExecutor", destroyMethod = "shutdown")
    ExecutorService postImageVirtualExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("image-upload-virtual-", 0).factory()
        );
    }

    @Bean
    Semaphore postImageRemoteCallSemaphore(PostImageUploadProperties properties) {
        return new Semaphore(properties.virtualMaxConcurrentTasks(), true);
    }

}
