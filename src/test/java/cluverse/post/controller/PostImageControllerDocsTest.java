package cluverse.post.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.post.service.PostImageService;
import cluverse.post.service.response.PostImageMultipartUploadResponse;
import cluverse.post.service.response.PostImagePresignedUrlResponse;
import cluverse.post.service.response.UploadedPostImageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostImageControllerDocsTest extends RestDocsSupport {

    private final PostImageService postImageService = mock(PostImageService.class);

    @Override
    protected Object initController() {
        return new PostImageController(postImageService);
    }

    @Test
    void 게시글_이미지_프리사인드_URL_발급() throws Exception {
        when(postImageService.createPresignedUrl(anyLong(), any())).thenReturn(
                new PostImagePresignedUrlResponse(
                        "posts/1/20260313-uuid.png",
                        "https://upload.example.com/presigned-url",
                        "https://cdn.example.com/posts/1/20260313-uuid.png",
                        LocalDateTime.of(2026, 3, 13, 12, 30)
                )
        );

        mockMvc.perform(post("/api/v1/post-images/presigned-urls")
                        .session(createSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "originalFileName": "study-banner.png",
                                    "contentType": "image/png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileKey").value("posts/1/20260313-uuid.png"))
                .andDo(document("post-images/create-presigned-url",
                        requestFields(
                                fieldWithPath("originalFileName").type(JsonFieldType.STRING).description("원본 파일명"),
                                fieldWithPath("contentType").type(JsonFieldType.STRING).description("업로드할 이미지의 MIME 타입")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.fileKey").type(JsonFieldType.STRING).description("저장소에 저장될 파일 키"),
                                fieldWithPath("data.uploadUrl").type(JsonFieldType.STRING).description("S3 presigned 업로드 URL"),
                                fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("업로드 완료 후 접근할 이미지 URL"),
                                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("presigned URL 만료 시각")
                        )
                ));
    }

    @Test
    void 게시글_이미지를_multipart로_업로드한다() throws Exception {
        when(postImageService.upload(anyLong(), any())).thenReturn(new PostImageMultipartUploadResponse(
                List.of(new UploadedPostImageResponse(
                        "posts/1/2026/08/06/image.png",
                        "https://cdn.example.com/posts/1/2026/08/06/image.png"
                ))
        ));
        MockMultipartFile image = new MockMultipartFile(
                "images", "image.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/post-images")
                        .file(image)
                        .session(createSession()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.images[0].fileKey")
                        .value("posts/1/2026/08/06/image.png"))
                .andExpect(jsonPath("$.data.images[0].imageUrl")
                        .value("https://cdn.example.com/posts/1/2026/08/06/image.png"))
                .andDo(document("post-images/upload-multipart",
                        requestParts(
                                partWithName("images").description("업로드할 이미지 파일(최대 5개)")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.images").type(JsonFieldType.ARRAY).description("업로드된 이미지 목록"),
                                fieldWithPath("data.images[].fileKey").type(JsonFieldType.STRING).description("저장소 파일 키"),
                                fieldWithPath("data.images[].imageUrl").type(JsonFieldType.STRING).description("게시글에 첨부할 이미지 URL")
                        )
                ));
    }

    private MockHttpSession createSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
