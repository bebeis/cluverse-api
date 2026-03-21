package cluverse.recruitment.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.member.domain.MemberRole;
import cluverse.recruitment.domain.RecruitmentApplicationStatus;
import cluverse.recruitment.service.RecruitmentApplicationService;
import cluverse.recruitment.service.RecruitmentApplicationQueryService;
import cluverse.recruitment.service.request.ApplicationChatMessageCreateRequest;
import cluverse.recruitment.service.request.RecruitmentApplicationStatusUpdateRequest;
import cluverse.recruitment.service.response.ApplicationChatMessagePageResponse;
import cluverse.recruitment.service.response.ApplicationChatMessageResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationAnswerResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationDetailResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationPageResponse;
import cluverse.recruitment.service.response.RecruitmentApplicationSummaryResponse;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecruitmentApplicationControllerDocsTest extends RestDocsSupport {

    private final RecruitmentApplicationQueryService recruitmentApplicationQueryService = mock(RecruitmentApplicationQueryService.class);
    private final RecruitmentApplicationService recruitmentApplicationService = mock(RecruitmentApplicationService.class);

    @Override
    protected Object initController() {
        return new RecruitmentApplicationController(recruitmentApplicationQueryService, recruitmentApplicationService);
    }

    @Test
    void 내_지원서_목록_조회() throws Exception {
        // given
        when(recruitmentApplicationQueryService.getMyApplications(eq(1L), any())).thenReturn(new RecruitmentApplicationPageResponse(
                List.of(createApplicationSummaryResponse()),
                1,
                20,
                false
        ));

        // when, then
        mockMvc.perform(get("/api/v1/recruitment-applications/me")
                        .session(createMemberSession())
                        .queryParam("status", "SUBMITTED")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applications[0].applicationId").value(30))
                .andDo(document("recruitment-applications/get-my-applications",
                        queryParameters(
                                parameterWithName("status").description("지원 상태").optional(),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(applicationPageResponseFields())
                ));
    }

    @Test
    void 모집글_지원서_목록_조회() throws Exception {
        // given
        when(recruitmentApplicationQueryService.getApplications(eq(1L), eq(10L), any()))
                .thenReturn(new RecruitmentApplicationPageResponse(
                        List.of(createApplicationSummaryResponse()),
                        1,
                        20,
                        false
                ));

        // when, then
        mockMvc.perform(get("/api/v1/recruitment-applications")
                        .session(createMemberSession())
                        .queryParam("recruitmentId", "10")
                        .queryParam("status", "IN_REVIEW")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applications[0].applicationId").value(30))
                .andDo(document("recruitment-applications/get-recruitment-applications",
                        queryParameters(
                                parameterWithName("recruitmentId").description("지원서를 조회할 모집글 ID"),
                                parameterWithName("status").description("지원 상태").optional(),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(applicationPageResponseFields())
                ));
    }

    @Test
    void 지원서_제출() throws Exception {
        // given
        when(recruitmentApplicationService.createApplication(eq(1L), eq(10L), any(), eq("127.0.0.1")))
                .thenReturn(30L);
        when(recruitmentApplicationQueryService.getApplication(1L, 30L)).thenReturn(createApplicationDetailResponse());

        // when, then
        mockMvc.perform(post("/api/v1/recruitment-applications")
                        .session(createMemberSession())
                        .queryParam("recruitmentId", "10")
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
                        queryParameters(
                                parameterWithName("recruitmentId").description("지원할 모집글 ID")
                        ),
                        requestFields(
                                fieldWithPath("position").type(JsonFieldType.STRING).description("지원 포지션").optional(),
                                fieldWithPath("portfolioUrl").type(JsonFieldType.STRING).description("포트폴리오 URL").optional(),
                                fieldWithPath("answers").type(JsonFieldType.ARRAY).description("질문 답변 목록").optional(),
                                fieldWithPath("answers[].formItemId").type(JsonFieldType.NUMBER).description("질문 ID"),
                                fieldWithPath("answers[].answer").type(JsonFieldType.STRING).description("답변 내용")
                        ),
                        responseFields(applicationDetailResponseFields())
                ));
    }

    @Test
    void 지원서_상세_조회() throws Exception {
        // given
        when(recruitmentApplicationQueryService.getApplication(1L, 30L)).thenReturn(createReviewedApplicationDetailResponse());

        // when, then
        mockMvc.perform(get("/api/v1/recruitment-applications/{applicationId}", 30L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(30))
                .andDo(document("recruitment-applications/get-application",
                        pathParameters(
                                parameterWithName("applicationId").description("조회할 지원서 ID")
                        ),
                        responseFields(applicationDetailResponseFields())
                ));
    }

    @Test
    void 지원서_상태_변경() throws Exception {
        // given
        when(recruitmentApplicationService.updateApplicationStatus(
                eq(1L),
                eq(30L),
                any(RecruitmentApplicationStatusUpdateRequest.class),
                eq("127.0.0.1")
        )).thenReturn(30L);
        when(recruitmentApplicationQueryService.getApplication(1L, 30L)).thenReturn(createApprovedApplicationDetailResponse());

        // when, then
        mockMvc.perform(patch("/api/v1/recruitment-applications/{applicationId}/status", 30L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "APPROVED",
                                    "note": "합류를 환영합니다."
                                }
                                """)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andDo(document("recruitment-applications/update-application-status",
                        pathParameters(
                                parameterWithName("applicationId").description("상태를 변경할 지원서 ID")
                        ),
                        requestFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("변경할 지원 상태"),
                                fieldWithPath("note").type(JsonFieldType.STRING).description("검토 메모").optional()
                        ),
                        responseFields(applicationDetailResponseFields())
                ));
    }

    @Test
    void 지원서_취소() throws Exception {
        // given
        doNothing().when(recruitmentApplicationService).cancelApplication(1L, 30L, "127.0.0.1");

        // when, then
        mockMvc.perform(delete("/api/v1/recruitment-applications/{applicationId}", 30L)
                        .session(createMemberSession())
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andDo(document("recruitment-applications/cancel-application",
                        pathParameters(
                                parameterWithName("applicationId").description("취소할 지원서 ID")
                        ),
                        responseFields(voidResponseFields())
                ));
    }

    @Test
    void 지원_채팅_메시지_목록_조회() throws Exception {
        // given
        when(recruitmentApplicationQueryService.getMessages(eq(1L), eq(30L), any()))
                .thenReturn(new ApplicationChatMessagePageResponse(
                        List.of(createMessageResponse()),
                        100L,
                        20,
                        false
                ));

        // when, then
        mockMvc.perform(get("/api/v1/recruitment-applications/{applicationId}/messages", 30L)
                        .session(createMemberSession())
                        .queryParam("beforeMessageId", "100")
                        .queryParam("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].applicationChatMessageId").value(101))
                .andDo(document("recruitment-applications/get-messages",
                        pathParameters(
                                parameterWithName("applicationId").description("메시지를 조회할 지원서 ID")
                        ),
                        queryParameters(
                                parameterWithName("beforeMessageId").description("이 ID 이전 메시지부터 조회").optional(),
                                parameterWithName("limit").description("조회 건수").optional()
                        ),
                        responseFields(messagePageResponseFields())
                ));
    }

    @Test
    void 지원_채팅_메시지_전송() throws Exception {
        // given
        when(recruitmentApplicationService.createMessage(eq(1L), eq(30L), any(ApplicationChatMessageCreateRequest.class), eq("127.0.0.1")))
                .thenReturn(101L);
        when(recruitmentApplicationQueryService.getMessage(1L, 30L, 101L)).thenReturn(createMessageResponse());

        // when, then
        mockMvc.perform(post("/api/v1/recruitment-applications/{applicationId}/messages", 30L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "포트폴리오 관련 질문 있습니다."
                                }
                                """)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.applicationChatMessageId").value(101))
                .andDo(document("recruitment-applications/create-message",
                        pathParameters(
                                parameterWithName("applicationId").description("메시지를 전송할 지원서 ID")
                        ),
                        requestFields(
                                fieldWithPath("content").type(JsonFieldType.STRING).description("메시지 내용")
                        ),
                        responseFields(messageSingleResponseFields())
                ));
    }

    private MockHttpSession createMemberSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private RecruitmentApplicationSummaryResponse createApplicationSummaryResponse() {
        return new RecruitmentApplicationSummaryResponse(
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
                LocalDateTime.of(2026, 3, 16, 10, 0),
                null
        );
    }

    private RecruitmentApplicationDetailResponse createApplicationDetailResponse() {
        return new RecruitmentApplicationDetailResponse(
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
        );
    }

    private RecruitmentApplicationDetailResponse createReviewedApplicationDetailResponse() {
        return new RecruitmentApplicationDetailResponse(
                30L,
                10L,
                1L,
                "백엔드 모집",
                1L,
                "luna",
                "https://cdn.example.com/profile.png",
                "Backend",
                "https://portfolio.example.com",
                RecruitmentApplicationStatus.IN_REVIEW,
                2L,
                "nova",
                LocalDateTime.of(2026, 3, 17, 14, 0),
                "추가 포트폴리오 검토 중",
                List.of(new RecruitmentApplicationAnswerResponse(1L, "지원 동기를 적어주세요.", "같이 만들고 싶습니다.")),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 17, 14, 0)
        );
    }

    private RecruitmentApplicationDetailResponse createApprovedApplicationDetailResponse() {
        return new RecruitmentApplicationDetailResponse(
                30L,
                10L,
                1L,
                "백엔드 모집",
                1L,
                "luna",
                "https://cdn.example.com/profile.png",
                "Backend",
                "https://portfolio.example.com",
                RecruitmentApplicationStatus.APPROVED,
                2L,
                "nova",
                LocalDateTime.of(2026, 3, 18, 18, 0),
                "합류를 환영합니다.",
                List.of(new RecruitmentApplicationAnswerResponse(1L, "지원 동기를 적어주세요.", "같이 만들고 싶습니다.")),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 18, 18, 0)
        );
    }

    private ApplicationChatMessageResponse createMessageResponse() {
        return new ApplicationChatMessageResponse(
                101L,
                30L,
                1L,
                "luna",
                "https://cdn.example.com/profile.png",
                "포트폴리오 관련 질문 있습니다.",
                true,
                true,
                LocalDateTime.of(2026, 3, 18, 19, 0)
        );
    }

    private FieldDescriptor[] applicationPageResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.applications").type(JsonFieldType.ARRAY).description("지원서 목록"),
                fieldWithPath("data.applications[].applicationId").type(JsonFieldType.NUMBER).description("지원서 ID"),
                fieldWithPath("data.applications[].recruitmentId").type(JsonFieldType.NUMBER).description("모집글 ID"),
                fieldWithPath("data.applications[].groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("data.applications[].recruitmentTitle").type(JsonFieldType.STRING).description("모집글 제목").optional(),
                fieldWithPath("data.applications[].applicantId").type(JsonFieldType.NUMBER).description("지원자 회원 ID"),
                fieldWithPath("data.applications[].applicantNickname").type(JsonFieldType.STRING).description("지원자 닉네임").optional(),
                fieldWithPath("data.applications[].applicantProfileImageUrl").type(JsonFieldType.STRING).description("지원자 프로필 이미지 URL").optional(),
                fieldWithPath("data.applications[].position").type(JsonFieldType.STRING).description("지원 포지션").optional(),
                fieldWithPath("data.applications[].portfolioUrl").type(JsonFieldType.STRING).description("포트폴리오 URL").optional(),
                fieldWithPath("data.applications[].status").type(JsonFieldType.STRING).description("지원 상태"),
                fieldWithPath("data.applications[].createdAt").type(JsonFieldType.STRING).description("지원 일시"),
                fieldWithPath("data.applications[].reviewedAt").type(JsonFieldType.STRING).description("검토 일시").optional(),
                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
        };
    }

    private FieldDescriptor[] applicationDetailResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.applicationId").type(JsonFieldType.NUMBER).description("지원서 ID"),
                fieldWithPath("data.recruitmentId").type(JsonFieldType.NUMBER).description("모집글 ID"),
                fieldWithPath("data.groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("data.recruitmentTitle").type(JsonFieldType.STRING).description("모집글 제목").optional(),
                fieldWithPath("data.applicantId").type(JsonFieldType.NUMBER).description("지원자 회원 ID"),
                fieldWithPath("data.applicantNickname").type(JsonFieldType.STRING).description("지원자 닉네임").optional(),
                fieldWithPath("data.applicantProfileImageUrl").type(JsonFieldType.STRING).description("지원자 프로필 이미지 URL").optional(),
                fieldWithPath("data.position").type(JsonFieldType.STRING).description("지원 포지션").optional(),
                fieldWithPath("data.portfolioUrl").type(JsonFieldType.STRING).description("포트폴리오 URL").optional(),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("지원 상태"),
                fieldWithPath("data.reviewedBy").type(JsonFieldType.NUMBER).description("검토자 회원 ID").optional(),
                fieldWithPath("data.reviewerNickname").type(JsonFieldType.STRING).description("검토자 닉네임").optional(),
                fieldWithPath("data.reviewedAt").type(JsonFieldType.STRING).description("검토 일시").optional(),
                fieldWithPath("data.latestReviewNote").type(JsonFieldType.STRING).description("최근 검토 메모").optional(),
                fieldWithPath("data.answers").type(JsonFieldType.ARRAY).description("답변 목록"),
                fieldWithPath("data.answers[].formItemId").type(JsonFieldType.NUMBER).description("질문 ID"),
                fieldWithPath("data.answers[].question").type(JsonFieldType.STRING).description("질문 내용").optional(),
                fieldWithPath("data.answers[].answer").type(JsonFieldType.STRING).description("답변 내용"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정 일시")
        };
    }

    private FieldDescriptor[] messagePageResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.messages").type(JsonFieldType.ARRAY).description("메시지 목록"),
                fieldWithPath("data.messages[].applicationChatMessageId").type(JsonFieldType.NUMBER).description("메시지 ID"),
                fieldWithPath("data.messages[].applicationId").type(JsonFieldType.NUMBER).description("지원서 ID"),
                fieldWithPath("data.messages[].senderId").type(JsonFieldType.NUMBER).description("발신자 회원 ID"),
                fieldWithPath("data.messages[].senderNickname").type(JsonFieldType.STRING).description("발신자 닉네임").optional(),
                fieldWithPath("data.messages[].senderProfileImageUrl").type(JsonFieldType.STRING).description("발신자 프로필 이미지 URL").optional(),
                fieldWithPath("data.messages[].content").type(JsonFieldType.STRING).description("메시지 내용"),
                fieldWithPath("data.messages[].isMine").type(JsonFieldType.BOOLEAN).description("내 메시지 여부"),
                fieldWithPath("data.messages[].isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                fieldWithPath("data.messages[].createdAt").type(JsonFieldType.STRING).description("전송 일시"),
                fieldWithPath("data.beforeMessageId").type(JsonFieldType.NUMBER).description("이전 페이지 조회 기준 메시지 ID").optional(),
                fieldWithPath("data.limit").type(JsonFieldType.NUMBER).description("조회 건수"),
                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("이전 메시지 존재 여부")
        };
    }

    private FieldDescriptor[] messageSingleResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.applicationChatMessageId").type(JsonFieldType.NUMBER).description("메시지 ID"),
                fieldWithPath("data.applicationId").type(JsonFieldType.NUMBER).description("지원서 ID"),
                fieldWithPath("data.senderId").type(JsonFieldType.NUMBER).description("발신자 회원 ID"),
                fieldWithPath("data.senderNickname").type(JsonFieldType.STRING).description("발신자 닉네임").optional(),
                fieldWithPath("data.senderProfileImageUrl").type(JsonFieldType.STRING).description("발신자 프로필 이미지 URL").optional(),
                fieldWithPath("data.content").type(JsonFieldType.STRING).description("메시지 내용"),
                fieldWithPath("data.isMine").type(JsonFieldType.BOOLEAN).description("내 메시지 여부"),
                fieldWithPath("data.isRead").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("전송 일시")
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
