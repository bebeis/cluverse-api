package cluverse.post.client;

import cluverse.common.exception.ExternalServiceException;
import cluverse.common.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Component
public class S3PostImageObjectStorageClient implements PostImageObjectStorageClient {

    private static final Duration READ_URL_DURATION = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner publicS3Presigner;
    private final AwsProperties awsProperties;

    public S3PostImageObjectStorageClient(
            S3Client s3Client,
            @Qualifier("publicS3Presigner") S3Presigner publicS3Presigner,
            AwsProperties awsProperties
    ) {
        this.s3Client = s3Client;
        this.publicS3Presigner = publicS3Presigner;
        this.awsProperties = awsProperties;
    }

    @Override
    public void upload(String objectKey, String contentType, Path source) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(awsProperties.s3().bucket())
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromFile(source)
            );
        } catch (SdkException exception) {
            throw new ExternalServiceException("이미지 원본을 staging 저장소에 업로드하지 못했습니다.", exception);
        }
    }

    @Override
    public void delete(Collection<String> objectKeys) {
        List<ObjectIdentifier> objects = objectKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
        if (objects.isEmpty()) {
            return;
        }
        DeleteObjectsResponse response;
        try {
            response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .delete(Delete.builder().objects(objects).quiet(true).build())
                    .build());
        } catch (SdkException exception) {
            throw new ExternalServiceException("이미지 객체를 삭제하지 못했습니다.", exception);
        }
        if (!response.errors().isEmpty()) {
            throw new ExternalServiceException(
                    "이미지 객체를 삭제하지 못했습니다.",
                    new IllegalStateException("S3 delete errors=" + response.errors().size())
            );
        }
    }

    @Override
    public long size(String objectKey) {
        try {
            return head(objectKey).contentLength();
        } catch (SdkException exception) {
            throw new ExternalServiceException("이미지 객체 크기를 확인하지 못했습니다.", exception);
        }
    }

    @Override
    public String createImageUrl(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(objectKey)
                .build();
        return publicS3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(READ_URL_DURATION)
                        .getObjectRequest(request)
                        .build())
                .url()
                .toString();
    }

    private software.amazon.awssdk.services.s3.model.HeadObjectResponse head(String objectKey) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(objectKey)
                .build());
    }
}
