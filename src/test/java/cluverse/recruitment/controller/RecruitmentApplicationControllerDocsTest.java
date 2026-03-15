package cluverse.recruitment.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.service.RecruitmentApplicationService;
import cluverse.recruitment.service.response.RecruitmentApplicationAnswerResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecruitmentApplicationControllerDocsTest extends RestDocsSupport {

    private final RecruitmentApplicationService recruitmentApplicationService = mock(RecruitmentApplicationService.class);

    @Override
    protected Object initController() {
        return new RecruitmentApplicationController(recruitmentApplicationService);
    }

    @Test
    void 지원서_제출() throws Exception {
        // given
        when(recruitmentApplicationService.createApplication(eq(1L), eq(10L), any(), eq("127.0.0.1")))
                .thenReturn(new RecruitmentApplicationDetailResponse(
                        30L,
                        10L,
                        1L,
                        "백엔드 모집",
                        1L,
                        "luna",
                        "https://cdn.example.com/profile.png",
                        "Backend",
                        "https://portfolio.example.com",
                        RecruitmentApplicationStatus.SUBMITTED,
                        null,
                        null,
                        null,
                        null,
                        List.of(new RecruitmentApplicationAnswerResponse(1L, "지원 동기를 적어주세요.", "같이 만들고 싶습니다.")),
                        LocalDateTime.of(2026, 3, 16, 10, 0),
                        LocalDateTime.of(2026, 3, 16, 10, 0)
                ));

        // when, then
        mockMvc.perform(post("/api/v1/recruitments/{recruitmentId}/applications", 10L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "position": "Backend",
                                    "portfolioUrl": "https://portfolio.example.com",
                                    "answers": [
                                        {
                                            "formItemId": 1,
                                            "answer": "같이 만들고 싶습니다."
                                        }
                                    ]
                                }
                                """)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applicationId").value(30))
                .andDo(document("recruitment-applications/create",
                        pathParameters(
                                parameterWithName("recruitmentId").description("지원할 모집글 ID")
                        ),
                        requestFields(
                                fieldWithPath("position").type(JsonFieldType.STRING).description("지원 포지션").optional(),
                                fieldWithPath("portfolioUrl").type(JsonFieldType.STRING).description("포트폴리오 URL").optional(),
                                fieldWithPath("answers").type(JsonFieldType.ARRAY).description("질문 답변 목록").optional(),
                                fieldWithPath("answers[].formItemId").type(JsonFieldType.NUMBER).description("질문 ID"),
                                fieldWithPath("answers[].answer").type(JsonFieldType.STRING).description("답변 내용")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.applicationId").type(JsonFieldType.NUMBER).description("지원서 ID"),
                                fieldWithPath("data.recruitmentId").type(JsonFieldType.NUMBER).description("모집글 ID"),
                                fieldWithPath("data.groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                                fieldWithPath("data.recruitmentTitle").type(JsonFieldType.STRING).description("모집글 제목"),
                                fieldWithPath("data.applicantId").type(JsonFieldType.NUMBER).description("지원자 회원 ID"),
                                fieldWithPath("data.applicantNickname").type(JsonFieldType.STRING).description("지원자 닉네임").optional(),
                                fieldWithPath("data.applicantProfileImageUrl").type(JsonFieldType.STRING).description("지원자 프로필 이미지 URL").optional(),
                                fieldWithPath("data.position").type(JsonFieldType.STRING).description("지원 포지션").optional(),
                                fieldWithPath("data.portfolioUrl").type(JsonFieldType.STRING).description("포트폴리오 URL").optional(),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("지원 상태"),
                                fieldWithPath("data.reviewedBy").type(JsonFieldType.NULL).description("검토자 회원 ID"),
                                fieldWithPath("data.reviewerNickname").type(JsonFieldType.NULL).description("검토자 닉네임"),
                                fieldWithPath("data.reviewedAt").type(JsonFieldType.NULL).description("검토 일시"),
                                fieldWithPath("data.latestReviewNote").type(JsonFieldType.NULL).description("최근 검토 메모"),
                                fieldWithPath("data.answers").type(JsonFieldType.ARRAY).description("답변 목록"),
                                fieldWithPath("data.answers[].formItemId").type(JsonFieldType.NUMBER).description("질문 ID"),
                                fieldWithPath("data.answers[].question").type(JsonFieldType.STRING).description("질문 내용").optional(),
                                fieldWithPath("data.answers[].answer").type(JsonFieldType.STRING).description("답변 내용"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시")
                        )
                ));
    }

    private MockHttpSession createMemberSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
