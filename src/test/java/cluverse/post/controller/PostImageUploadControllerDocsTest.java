package cluverse.post.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.common.auth.LoginMember;
import cluverse.member.domain.MemberRole;
import cluverse.post.service.PostImageUploadService;
import cluverse.post.service.request.PostImageUploadRequest;
import cluverse.post.service.response.PostImageAssetResponse;
import cluverse.post.service.response.PostImageUploadResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockPart;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import org.springframework.restdocs.payload.JsonFieldType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;

class PostImageUploadControllerDocsTest extends RestDocsSupport {

    private final PostImageUploadService service = mock(PostImageUploadService.class);

    @Override
    protected Object initController() {
        return new PostImageUploadController(service);
    }

    @Test
    void 이미지_업로드를_완료한다() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(service.upload(anyLong(), any())).thenReturn(new PostImageUploadResponse(
                requestId,
                "COMPLETED",
                1_000,
                400,
                60.0,
                List.of(new PostImageAssetResponse(
                        0,
                        "content.jpg",
                        "https://images.example.com/content.jpg",
                        "thumbnail.jpg",
                        "https://images.example.com/thumbnail.jpg",
                        1_000,
                        320L,
                        80L
                ))
        ));
        MockMultipartFile image = new MockMultipartFile(
                "images", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        MockMultipartFile secondImage = new MockMultipartFile(
                "images", "second.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{4, 5, 6});
        MockPart requestIdPart = new MockPart(
                "requestId", requestId.toString().getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/image-uploads")
                        .file(image)
                        .file(secondImage)
                        .part(requestIdPart)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outputBytes").value(400))
                .andExpect(jsonPath("$.data.images[0].contentUrl")
                        .value("https://images.example.com/content.jpg"))
                .andExpect(jsonPath("$.data.images[0].thumbnailUrl")
                        .value("https://images.example.com/thumbnail.jpg"))
                .andDo(document("image-uploads/upload",
                        requestParts(
                                partWithName("requestId").description("멱등 업로드 요청 UUID"),
                                partWithName("images").description("외부 프로세서로 보낼 이미지 파일(최대 5개)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.requestId").type(JsonFieldType.STRING).description("멱등 요청 UUID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태"),
                                fieldWithPath("data.sourceBytes").type(JsonFieldType.NUMBER).description("원본 총 바이트"),
                                fieldWithPath("data.outputBytes").type(JsonFieldType.NUMBER).description("출력 총 바이트"),
                                fieldWithPath("data.reductionPercent").type(JsonFieldType.NUMBER).description("감소율"),
                                fieldWithPath("data.images").type(JsonFieldType.ARRAY).description("처리 이미지 목록"),
                                fieldWithPath("data.images[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서"),
                                fieldWithPath("data.images[].contentKey").type(JsonFieldType.STRING).description("본문 이미지 key"),
                                fieldWithPath("data.images[].contentUrl").type(JsonFieldType.STRING).description("본문 이미지 조회 URL"),
                                fieldWithPath("data.images[].thumbnailKey").type(JsonFieldType.STRING).description("썸네일 key"),
                                fieldWithPath("data.images[].thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 조회 URL"),
                                fieldWithPath("data.images[].sourceBytes").type(JsonFieldType.NUMBER).description("원본 바이트"),
                                fieldWithPath("data.images[].contentBytes").type(JsonFieldType.NUMBER).description("본문 이미지 바이트"),
                                fieldWithPath("data.images[].thumbnailBytes").type(JsonFieldType.NUMBER).description("썸네일 바이트")
                        )
                ));

        ArgumentCaptor<PostImageUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(PostImageUploadRequest.class);
        verify(service).upload(anyLong(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().images()).hasSize(2);
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
