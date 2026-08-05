package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.post.service.PostImageUploadService;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageAssetResponse;
import cluverse.post.service.response.PostImageUploadResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostImageUploadControllerV1DocsTest extends RestDocsSupport {

    private final PostImageUploadService service = mock(PostImageUploadService.class);

    @Override
    protected Object initController() {
        return new PostImageUploadControllerV1(service);
    }

    @Test
    void 외부_이미지_프로세서를_순차_호출한다() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(service.upload(any(), anyString(), any())).thenReturn(new PostImageUploadResponse(
                requestId,
                "v1",
                "COMPLETED",
                1_000,
                400,
                60.0,
                List.of(new PostImageAssetResponse(0, "content.jpg", "thumbnail.jpg", 1_000, 320L, 80L))
        ));
        MockMultipartFile image = new MockMultipartFile(
                "images", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        MockMultipartFile secondImage = new MockMultipartFile(
                "images", "second.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{4, 5, 6});
        MockPart requestIdPart = new MockPart(
                "requestId", requestId.toString().getBytes(StandardCharsets.UTF_8));
        MockPart failurePointPart = new MockPart(
                "failurePoint", "NONE".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/image-uploads")
                        .file(image)
                        .file(secondImage)
                        .part(requestIdPart)
                        .part(failurePointPart)
                        .header("X-Benchmark-Token", "secret"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value("v1"))
                .andExpect(jsonPath("$.data.outputBytes").value(400))
                .andDo(document("image-uploads/v1-upload",
                        requestHeaders(
                                headerWithName("X-Benchmark-Token")
                                        .description("테스트 배포에서 설정한 벤치마크 토큰")
                                        .optional()
                        ),
                        requestParts(
                                partWithName("requestId").description("멱등 업로드 요청 UUID"),
                                partWithName("failurePoint")
                                        .description("실패 주입 지점. 생략하면 NONE")
                                        .optional(),
                                partWithName("images").description("외부 프로세서로 보낼 이미지 파일(최대 5개)")
                        )
                ));

        ArgumentCaptor<PostImageUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(PostImageUploadRequest.class);
        verify(service).upload(any(), anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().images()).hasSize(2);
    }
}
