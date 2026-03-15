package cluverse.recruitment.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.recruitment.domain.RecruitmentStatus;
import cluverse.recruitment.service.RecruitmentService;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import cluverse.recruitment.service.response.RecruitmentFormItemResponse;
import cluverse.recruitment.service.response.RecruitmentPositionResponse;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecruitmentControllerDocsTest extends RestDocsSupport {

    private final RecruitmentService recruitmentService = mock(RecruitmentService.class);

    @Override
    protected Object initController() {
        return new RecruitmentController(recruitmentService);
    }

    @Test
    void 모집글_생성() throws Exception {
        // given
        when(recruitmentService.createRecruitment(eq(1L), eq(1L), any())).thenReturn(new RecruitmentDetailResponse(
                10L,
                1L,
                1L,
                "luna",
                "백엔드 모집",
                "스프링 백엔드 모집 공고",
                List.of(new RecruitmentPositionResponse("Backend", 2)),
                "Spring Boot 경험",
                "3개월",
                "MVP 출시",
                "주 2회 온라인 회의",
                LocalDateTime.of(2026, 3, 31, 23, 59),
                RecruitmentStatus.OPEN,
                0,
                List.of(new RecruitmentFormItemResponse(1L, "지원 동기를 적어주세요.", cluverse.recruitment.domain.FormItemQuestionType.TEXT, true, List.of(), 1)),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 10, 0)
        ));

        // when, then
        mockMvc.perform(post("/api/v1/recruitments")
                        .session(createMemberSession())
                        .queryParam("groupId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "백엔드 모집",
                                    "description": "스프링 백엔드 모집 공고",
                                    "positions": [
                                        {
                                            "name": "Backend",
                                            "count": 2
                                        }
                                    ],
                                    "requirements": "Spring Boot 경험",
                                    "duration": "3개월",
                                    "goal": "MVP 출시",
                                    "processDescription": "주 2회 온라인 회의",
                                    "deadline": "2026-03-31T23:59:00",
                                    "formItems": [
                                        {
                                            "question": "지원 동기를 적어주세요.",
                                            "questionType": "TEXT",
                                            "isRequired": true,
                                            "options": [],
                                            "displayOrder": 1
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recruitmentId").value(10))
                .andDo(document("recruitments/create",
                        queryParameters(
                                parameterWithName("groupId").description("모집글이 속한 그룹 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("모집글 제목"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("모집글 소개"),
                                fieldWithPath("positions").type(JsonFieldType.ARRAY).description("모집 포지션 목록").optional(),
                                fieldWithPath("positions[].name").type(JsonFieldType.STRING).description("포지션명"),
                                fieldWithPath("positions[].count").type(JsonFieldType.NUMBER).description("모집 인원"),
                                fieldWithPath("requirements").type(JsonFieldType.STRING).description("요구 조건").optional(),
                                fieldWithPath("duration").type(JsonFieldType.STRING).description("활동 기간").optional(),
                                fieldWithPath("goal").type(JsonFieldType.STRING).description("목표").optional(),
                                fieldWithPath("processDescription").type(JsonFieldType.STRING).description("진행 방식").optional(),
                                fieldWithPath("deadline").type(JsonFieldType.STRING).description("마감 일시").optional(),
                                fieldWithPath("formItems").type(JsonFieldType.ARRAY).description("지원서 질문 목록").optional(),
                                fieldWithPath("formItems[].question").type(JsonFieldType.STRING).description("질문 내용"),
                                fieldWithPath("formItems[].questionType").type(JsonFieldType.STRING).description("질문 유형"),
                                fieldWithPath("formItems[].isRequired").type(JsonFieldType.BOOLEAN).description("필수 여부"),
                                fieldWithPath("formItems[].options").type(JsonFieldType.ARRAY).description("선택형 옵션 목록"),
                                fieldWithPath("formItems[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.recruitmentId").type(JsonFieldType.NUMBER).description("모집글 ID"),
                                fieldWithPath("data.groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                                fieldWithPath("data.authorId").type(JsonFieldType.NUMBER).description("작성자 회원 ID"),
                                fieldWithPath("data.authorNickname").type(JsonFieldType.STRING).description("작성자 닉네임").optional(),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("모집글 제목"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("모집글 소개"),
                                fieldWithPath("data.positions").type(JsonFieldType.ARRAY).description("모집 포지션 목록"),
                                fieldWithPath("data.positions[].name").type(JsonFieldType.STRING).description("포지션명"),
                                fieldWithPath("data.positions[].count").type(JsonFieldType.NUMBER).description("모집 인원"),
                                fieldWithPath("data.requirements").type(JsonFieldType.STRING).description("요구 조건").optional(),
                                fieldWithPath("data.duration").type(JsonFieldType.STRING).description("활동 기간").optional(),
                                fieldWithPath("data.goal").type(JsonFieldType.STRING).description("목표").optional(),
                                fieldWithPath("data.processDescription").type(JsonFieldType.STRING).description("진행 방식").optional(),
                                fieldWithPath("data.deadline").type(JsonFieldType.STRING).description("마감 일시").optional(),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("모집 상태"),
                                fieldWithPath("data.applicationCount").type(JsonFieldType.NUMBER).description("지원 수"),
                                fieldWithPath("data.formItems").type(JsonFieldType.ARRAY).description("지원서 질문 목록"),
                                fieldWithPath("data.formItems[].formItemId").type(JsonFieldType.NUMBER).description("질문 ID"),
                                fieldWithPath("data.formItems[].question").type(JsonFieldType.STRING).description("질문 내용"),
                                fieldWithPath("data.formItems[].questionType").type(JsonFieldType.STRING).description("질문 유형"),
                                fieldWithPath("data.formItems[].isRequired").type(JsonFieldType.BOOLEAN).description("필수 여부"),
                                fieldWithPath("data.formItems[].options").type(JsonFieldType.ARRAY).description("선택형 옵션 목록"),
                                fieldWithPath("data.formItems[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서"),
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
