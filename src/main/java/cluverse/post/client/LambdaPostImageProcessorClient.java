package cluverse.post.client;

import cluverse.common.exception.ExternalServiceException;
import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.exception.PostImageUploadTimeoutException;
import cluverse.post.properties.PostImageUploadProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "image-upload-experiment",
        name = "processor-mode",
        havingValue = "lambda",
        matchIfMissing = true
)
public class LambdaPostImageProcessorClient implements PostImageProcessorClient {

    private final LambdaClient lambdaClient;
    private final PostImageUploadProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProcessedPostImage process(PostImageProcessCommand command) {
        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(properties.lambdaFunctionName())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(command)))
                    .overrideConfiguration(configuration -> configuration
                            .apiCallTimeout(properties.remoteTimeout()))
                    .build());
            validateResponse(response);
            return objectMapper.readValue(response.payload().asUtf8String(), ProcessedPostImage.class);
        } catch (ApiCallTimeoutException exception) {
            throw new PostImageUploadTimeoutException("이미지 프로세서 응답 시간이 초과됐습니다.", exception);
        } catch (JsonProcessingException exception) {
            throw new ExternalServiceException("이미지 프로세서 응답을 해석하지 못했습니다.", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof PostImageUploadTimeoutException) {
                throw exception;
            }
            throw new ExternalServiceException("이미지 프로세서를 호출하지 못했습니다.", exception);
        }
    }

    private void validateResponse(InvokeResponse response) {
        if (response.functionError() != null && !response.functionError().isBlank()) {
            throw new ExternalServiceException(
                    "이미지 프로세서가 실패했습니다: " + response.payload().asUtf8String(),
                    new IllegalStateException(response.functionError())
            );
        }
        if (response.statusCode() == null || response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ExternalServiceException(
                    "이미지 프로세서 호출 상태가 올바르지 않습니다.",
                    new IllegalStateException("status=" + response.statusCode())
            );
        }
    }
}
