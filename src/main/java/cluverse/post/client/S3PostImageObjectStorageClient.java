package cluverse.post.client;

import cluverse.common.exception.ExternalServiceException;
import cluverse.common.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class S3PostImageObjectStorageClient implements PostImageObjectStorageClient {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

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
        } catch (RuntimeException exception) {
            throw new ExternalServiceException("이미지 원본을 staging 저장소에 업로드하지 못했습니다.", exception);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey, String contentType) {
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .copySource(awsProperties.s3().bucket() + "/" + sourceKey)
                    .destinationBucket(awsProperties.s3().bucket())
                    .destinationKey(targetKey)
                    .metadataDirective(MetadataDirective.REPLACE)
                    .contentType(contentType)
                    .build());
        } catch (RuntimeException exception) {
            throw new ExternalServiceException("mock 이미지 결과 객체를 저장하지 못했습니다.", exception);
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
        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .delete(Delete.builder().objects(objects).quiet(true).build())
                    .build());
            if (!response.errors().isEmpty()) {
                throw new IllegalStateException("S3 delete errors=" + response.errors().size());
            }
        } catch (RuntimeException exception) {
            throw new ExternalServiceException("이미지 객체를 삭제하지 못했습니다.", exception);
        }
    }

    @Override
    public long size(String objectKey) {
        try {
            return head(objectKey).contentLength();
        } catch (RuntimeException exception) {
            throw new ExternalServiceException("이미지 객체 크기를 확인하지 못했습니다.", exception);
        }
    }

    private software.amazon.awssdk.services.s3.model.HeadObjectResponse head(String objectKey) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(objectKey)
                .build());
    }
}
