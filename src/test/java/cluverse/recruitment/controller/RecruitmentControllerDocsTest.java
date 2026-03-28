package cluverse.recruitment.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.recruitment.domain.FormItemQuestionType;
import cluverse.recruitment.domain.RecruitmentStatus;
import cluverse.recruitment.service.RecruitmentService;
import cluverse.recruitment.service.RecruitmentQueryService;
import cluverse.recruitment.service.request.RecruitmentStatusUpdateRequest;
import cluverse.recruitment.service.request.RecruitmentUpdateRequest;
import cluverse.recruitment.service.response.RecruitmentDetailResponse;
import cluverse.recruitment.service.response.RecruitmentFormItemResponse;
import cluverse.recruitment.service.response.RecruitmentPageResponse;
import cluverse.recruitment.service.response.RecruitmentPositionResponse;
import cluverse.recruitment.service.response.RecruitmentSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecruitmentControllerDocsTest extends RestDocsSupport {

    private final RecruitmentQueryService recruitmentQueryService = mock(RecruitmentQueryService.class);
    private final RecruitmentService recruitmentService = mock(RecruitmentService.class);

    @Override
    protected Object initController() {
        return new RecruitmentController(recruitmentQueryService, recruitmentService);
    }

    @Test
    void 모집글_목록_조회() throws Exception {
        // given
        when(recruitmentQueryService.getRecruitments(eq(1L), any())).thenReturn(new RecruitmentPageResponse(
                List.of(createRecruitmentSummaryResponse()),
                1,
                20,
                false
        ));

        // when, then
        mockMvc.perform(get("/api/v1/recruitments")
                        .session(createMemberSession())
                        .queryParam("groupId", "1")
                        .queryParam("status", "OPEN")
                        .queryParam("recruitingOnly", "true")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recruitments[0].recruitmentId").value(10))
                .andDo(document("recruitments/get-recruitment-list",
                        queryParameters(
                                parameterWithName("groupId").description("그룹 ID").optional(),
                                parameterWithName("status").description("모집 상태").optional(),
                                parameterWithName("recruitingOnly").description("모집 중인 글만 조회할지 여부").optional(),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(recruitmentPageResponseFields())
                ));
    }

    @Test
    void 모집글_생성() throws Exception {
        // given
        when(recruitmentService.createRecruitment(eq(1L), eq(1L), any())).thenReturn(createRecruitmentDetailResponse());

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
                        requestFields(recruitmentRequestFields()),
                        responseFields(recruitmentDetailResponseFields())
                ));
    }

    @Test
    void 모집글_상세_조회() throws Exception {
        // given
        when(recruitmentQueryService.getRecruitment(1L, 10L)).thenReturn(createRecruitmentDetailResponse());

        // when, then
        mockMvc.perform(get("/api/v1/recruitments/{recruitmentId}", 10L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recruitmentId").value(10))
                .andDo(document("recruitments/get-recruitment",
                        pathParameters(
                                parameterWithName("recruitmentId").description("조회할 모집글 ID")
                        ),
                        responseFields(recruitmentDetailResponseFields())
                ));
    }

    @Test
    void 모집글_수정() throws Exception {
        // given
        when(recruitmentService.updateRecruitment(eq(1L), eq(10L), any(RecruitmentUpdateRequest.class)))
                .thenReturn(createUpdatedRecruitmentDetailResponse());

        // when, then
        mockMvc.perform(put("/api/v1/recruitments/{recruitmentId}", 10L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "백엔드 모집 시즌2",
                                    "description": "플랫폼 백엔드 모집 공고",
                                    "positions": [
                                        {
                                            "name": "Backend",
                                            "count": 3
                                        }
                                    ],
                                    "requirements": "Spring Boot, AWS 경험",
                                    "duration": "4개월",
                                    "goal": "정식 출시",
                                    "processDescription": "주 3회 온라인 회의",
                                    "deadline": "2026-04-15T23:59:00",
                                    "formItems": [
                                        {
                                            "question": "협업 경험을 적어주세요.",
                                            "questionType": "TEXT",
                                            "isRequired": true,
                                            "options": [],
                                            "displayOrder": 1
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("백엔드 모집 시즌2"))
                .andDo(document("recruitments/update-recruitment",
                        pathParameters(
                                parameterWithName("recruitmentId").description("수정할 모집글 ID")
                        ),
                        requestFields(recruitmentRequestFields()),
                        responseFields(recruitmentDetailResponseFields())
                ));
    }

    @Test
    void 모집글_상태_변경() throws Exception {
        // given
        when(recruitmentService.updateRecruitmentStatus(eq(1L), eq(10L), any(RecruitmentStatusUpdateRequest.class)))
                .thenReturn(createClosedRecruitmentDetailResponse());

        // when, then
        mockMvc.perform(patch("/api/v1/recruitments/{recruitmentId}/status", 10L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andDo(document("recruitments/update-recruitment-status",
                        pathParameters(
                                parameterWithName("recruitmentId").description("상태를 변경할 모집글 ID")
                        ),
                        requestFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("변경할 모집 상태")
                        ),
                        responseFields(recruitmentDetailResponseFields())
                ));
    }

    @Test
    void 모집글_삭제() throws Exception {
        // given
        doNothing().when(recruitmentService).deleteRecruitment(1L, 10L);

        // when, then
        mockMvc.perform(delete("/api/v1/recruitments/{recruitmentId}", 10L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andDo(document("recruitments/delete-recruitment",
                        pathParameters(
                                parameterWithName("recruitmentId").description("삭제할 모집글 ID")
                        ),
                        responseFields(voidResponseFields())
                ));
    }

    private MockHttpSession createMemberSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private RecruitmentSummaryResponse createRecruitmentSummaryResponse() {
        return new RecruitmentSummaryResponse(
                10L,
                1L,
                "백엔드 모집",
                List.of(new RecruitmentPositionResponse("Backend", 2)),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                RecruitmentStatus.OPEN,
                4,
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
    }

    private RecruitmentDetailResponse createRecruitmentDetailResponse() {
        return new RecruitmentDetailResponse(
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
                List.of(
                        new RecruitmentFormItemResponse(
                                1L,
                                "지원 동기를 적어주세요.",
                                FormItemQuestionType.TEXT,
                                true,
                                List.of(),
                                1
                        )
                ),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
    }

    private RecruitmentDetailResponse createUpdatedRecruitmentDetailResponse() {
        return new RecruitmentDetailResponse(
                10L,
                1L,
                1L,
                "luna",
                "백엔드 모집 시즌2",
                "플랫폼 백엔드 모집 공고",
                List.of(new RecruitmentPositionResponse("Backend", 3)),
                "Spring Boot, AWS 경험",
                "4개월",
                "정식 출시",
                "주 3회 온라인 회의",
                LocalDateTime.of(2026, 4, 15, 23, 59),
                RecruitmentStatus.OPEN,
                2,
                List.of(
                        new RecruitmentFormItemResponse(
                                2L,
                                "협업 경험을 적어주세요.",
                                FormItemQuestionType.TEXT,
                                true,
                                List.of(),
                                1
                        )
                ),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 20, 10, 0)
        );
    }

    private RecruitmentDetailResponse createClosedRecruitmentDetailResponse() {
        return new RecruitmentDetailResponse(
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
                RecruitmentStatus.CLOSED,
                5,
                List.of(
                        new RecruitmentFormItemResponse(
                                1L,
                                "지원 동기를 적어주세요.",
                                FormItemQuestionType.TEXT,
                                true,
                                List.of(),
                                1
                        )
                ),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 21, 10, 0)
        );
    }

    private FieldDescriptor[] recruitmentRequestFields() {
        return new FieldDescriptor[]{
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
        };
    }

    private FieldDescriptor[] recruitmentPageResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.recruitments").type(JsonFieldType.ARRAY).description("모집글 목록"),
                fieldWithPath("data.recruitments[].recruitmentId").type(JsonFieldType.NUMBER).description("모집글 ID"),
                fieldWithPath("data.recruitments[].groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("data.recruitments[].title").type(JsonFieldType.STRING).description("모집글 제목"),
                fieldWithPath("data.recruitments[].positions").type(JsonFieldType.ARRAY).description("모집 포지션 목록"),
                fieldWithPath("data.recruitments[].positions[].name").type(JsonFieldType.STRING).description("포지션명"),
                fieldWithPath("data.recruitments[].positions[].count").type(JsonFieldType.NUMBER).description("모집 인원"),
                fieldWithPath("data.recruitments[].deadline").type(JsonFieldType.STRING).description("마감 일시").optional(),
                fieldWithPath("data.recruitments[].status").type(JsonFieldType.STRING).description("모집 상태"),
                fieldWithPath("data.recruitments[].applicationCount").type(JsonFieldType.NUMBER).description("지원 수"),
                fieldWithPath("data.recruitments[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
        };
    }

    private FieldDescriptor[] recruitmentDetailResponseFields() {
        return new FieldDescriptor[]{
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
        };
    }

    private FieldDescriptor[] voidResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 없음")
        };
    }
}
