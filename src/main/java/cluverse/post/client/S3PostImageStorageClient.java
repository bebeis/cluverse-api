package cluverse.post.client;

import cluverse.common.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3PostImageStorageClient implements PostImageStorageClient {

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    @Override
    public PresignedUploadResult createPresignedUpload(String fileKey, String contentType) {
        PresignedPutObjectRequest presignedRequest = createPresignedRequest(fileKey, contentType);
        return new PresignedUploadResult(
                presignedRequest.url().toString(),
                createImageUrl(fileKey)
        );
    }

    private PresignedPutObjectRequest createPresignedRequest(String fileKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(fileKey)
                .contentType(contentType)
                .build();

        return s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGNED_URL_DURATION)
                        .putObjectRequest(putObjectRequest)
                        .build()
        );
    }

    private String createImageUrl(String fileKey) {
        if (StringUtils.hasText(awsProperties.s3().endpoint())) {
            return "%s/%s/%s".formatted(
                    awsProperties.s3().endpoint(),
                    awsProperties.s3().bucket(),
                    fileKey
            );
        }
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                awsProperties.s3().bucket(),
                awsProperties.region(),
                fileKey
        );
    }
}
