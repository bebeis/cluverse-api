package cluverse.post.client;

import cluverse.post.domain.ProcessedPostImage;
import cluverse.post.properties.PostImageUploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LambdaPostImageProcessorClientTest {

    @Test
    void Lambda의_두_출력_metadata를_처리_결과로_변환한다() {
        LambdaClient lambdaClient = mock(LambdaClient.class);
        when(lambdaClient.invoke(any(software.amazon.awssdk.services.lambda.model.InvokeRequest.class)))
                .thenReturn(InvokeResponse.builder()
                        .statusCode(200)
                        .payload(SdkBytes.fromUtf8String("""
                                {
                                  "displayOrder": 0,
                                  "content": {
                                    "objectKey": "content/a.jpg",
                                    "contentType": "image/jpeg",
                                    "width": 1280,
                                    "height": 720,
                                    "bytes": 400
                                  },
                                  "thumbnail": {
                                    "objectKey": "thumbnail/a.jpg",
                                    "contentType": "image/jpeg",
                                    "width": 320,
                                    "height": 180,
                                    "bytes": 80
                                  }
                                }
                                """))
                        .build());
        LambdaPostImageProcessorClient client = new LambdaPostImageProcessorClient(
                lambdaClient,
                new PostImageUploadProperties(
                        true,
                        "token",
                        "image-processor",
                        "",
                        DataSize.ofMegabytes(10),
                        32,
                        16,
                        Duration.ofSeconds(30),
                        Duration.ofMinutes(3),
                        Duration.ofSeconds(30)
                )
        );
        PostImageProcessCommand command = new PostImageProcessCommand(
                UUID.randomUUID(), 0, "staging/a", "content/a.jpg", "thumbnail/a.jpg", "p1");

        ProcessedPostImage result = client.process(command);

        assertThat(result.content().bytes()).isEqualTo(400);
        assertThat(result.thumbnail().width()).isEqualTo(320);
    }
}
