package cluverse.post.client;

import cluverse.common.exception.ExternalServiceException;
import cluverse.common.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S3PostImageStorageClient implements PostImageStorageClient {

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(10);
    private static final String CUSTOM_ENDPOINT_IMAGE_URL_FORMAT = "%s/%s/%s";
    private static final String AWS_S3_IMAGE_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

    private final S3Client s3Client;
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

    @Override
    public void upload(String fileKey, String contentType, Path source) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(awsProperties.s3().bucket())
                            .key(fileKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromFile(source)
            );
        } catch (RuntimeException exception) {
            throw new ExternalServiceException("게시글 이미지를 저장소에 업로드하지 못했습니다.", exception);
        }
    }

    @Override
    public void delete(Collection<String> fileKeys) {
        List<ObjectIdentifier> objects = fileKeys.stream()
                .filter(fileKey -> fileKey != null && !fileKey.isBlank())
                .distinct()
                .map(fileKey -> ObjectIdentifier.builder().key(fileKey).build())
                .toList();
        if (objects.isEmpty()) {
            return;
        }
        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .delete(Delete.builder().objects(objects).quiet(true).build())
                    .build());
            if (!response.errors().isEmpty()) {
                throw new IllegalStateException("S3 delete errors=" + response.errors().size());
            }
        } catch (RuntimeException exception) {
            throw new ExternalServiceException("업로드에 실패한 게시글 이미지를 정리하지 못했습니다.", exception);
        }
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

    @Override
    public String createImageUrl(String fileKey) {
        if (StringUtils.hasText(awsProperties.s3().endpoint())) {
            return CUSTOM_ENDPOINT_IMAGE_URL_FORMAT.formatted(
                    awsProperties.s3().endpoint(),
                    awsProperties.s3().bucket(),
                    fileKey
            );
        }
        return AWS_S3_IMAGE_URL_FORMAT.formatted(
                awsProperties.s3().bucket(),
                awsProperties.region(),
                fileKey
        );
    }
}
