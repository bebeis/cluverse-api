package cluverse.post.service.implement;

import cluverse.common.exception.ForbiddenException;
import cluverse.post.properties.PostImageProcessorMode;
import cluverse.post.properties.PostImageUploadProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostImageUploadExperimentAuthorizerTest {

    @Test
    void stub은_Lambda_함수명_없이_실행할_수_있다() {
        PostImageUploadExperimentAuthorizer authorizer = new PostImageUploadExperimentAuthorizer(
                properties(PostImageProcessorMode.STUB, ""));

        assertThatCode(() -> authorizer.authorize("token")).doesNotThrowAnyException();
    }

    @Test
    void Lambda_mode는_함수명이_필수다() {
        PostImageUploadExperimentAuthorizer authorizer = new PostImageUploadExperimentAuthorizer(
                properties(PostImageProcessorMode.LAMBDA, ""));

        assertThatThrownBy(() -> authorizer.authorize("token"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("외부 이미지 프로세서가 설정되지 않았습니다.");
    }

    private PostImageUploadProperties properties(PostImageProcessorMode mode, String lambdaFunctionName) {
        return new PostImageUploadProperties(
                true,
                "token",
                mode,
                lambdaFunctionName,
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
