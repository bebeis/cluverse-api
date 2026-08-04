package cluverse.home.controller;

import cluverse.board.domain.BoardType;
import cluverse.certification.domain.CertificationExamPhase;
import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.home.service.HomeQueryService;
import cluverse.home.service.response.CertificationDeadlineResponse;
import cluverse.home.service.response.FavoriteBoardPageResponse;
import cluverse.home.service.response.FavoriteBoardResponse;
import cluverse.home.service.response.RecentCommentedPostResponse;
import cluverse.home.service.response.UsefulSiteResponse;
import cluverse.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerV1DocsTest extends RestDocsSupport {

    private final HomeQueryService homeQueryService = mock(HomeQueryService.class);

    @Override
    protected Object initController() {
        return new HomeControllerV1(homeQueryService);
    }

    @Test
    void 즐겨찾는_게시판_조회() throws Exception {
        when(homeQueryService.getFavoriteBoards(eq(1L), any())).thenReturn(new FavoriteBoardPageResponse(
                List.of(
                        new FavoriteBoardResponse(10L, BoardType.INTEREST, "AI", null),
                        new FavoriteBoardResponse(11L, BoardType.DEPARTMENT, "컴퓨터공학", 10L)
                ),
                11L,
                true
        ));

        mockMvc.perform(get("/api/v1/home/favorite-boards")
                        .session(session())
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boards[0].name").value("AI"))
                .andDo(document("home/get-favorite-boards",
                        queryParameters(
                                parameterWithName("cursor").description("이전 페이지의 마지막 게시판 ID").optional(),
                                parameterWithName("size").description("조회 크기, 기본 10·최대 20").optional()
                        ),
                        responseFields(fields(
                                fieldWithPath("data.boards").type(JsonFieldType.ARRAY).description("게시판 바로가기"),
                                fieldWithPath("data.boards[].boardId").type(JsonFieldType.NUMBER).description("게시판 ID"),
                                fieldWithPath("data.boards[].boardType").type(JsonFieldType.STRING).description("게시판 유형"),
                                fieldWithPath("data.boards[].name").type(JsonFieldType.STRING).description("게시판 이름"),
                                fieldWithPath("data.boards[].parentBoardId").type(JsonFieldType.VARIES).description("상위 게시판 ID, 최상위면 null").optional(),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.NUMBER).description("다음 커서").optional(),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                )));
    }

    @Test
    void 최근_댓글_글_개선_전_조회() throws Exception {
        when(homeQueryService.getRecentCommentedPostsV1(1L)).thenReturn(recentPosts());

        mockMvc.perform(get("/api/v1/home/recent-commented-posts").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postId").value(20))
                .andDo(document("home/get-recent-commented-posts-v1",
                        responseFields(recentPostFields())
                ));
    }

    @Test
    void 접수_마감_임박_자격시험_조회() throws Exception {
        when(homeQueryService.getUpcomingCertificationDeadlines()).thenReturn(List.of(
                new CertificationDeadlineResponse(
                        "국가기술자격",
                        "2026년 정기 기사 1회",
                        CertificationExamPhase.WRITTEN,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 21)
                )
        ));

        mockMvc.perform(get("/api/v1/home/certification-deadlines").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].phase").value("WRITTEN"))
                .andDo(document("home/get-certification-deadlines",
                        responseFields(fields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("접수 마감 임박순 시험"),
                                fieldWithPath("data[].qualificationType").type(JsonFieldType.STRING).description("자격 구분"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).description("시험 일정명"),
                                fieldWithPath("data[].phase").type(JsonFieldType.STRING).description("필기·실기 구분"),
                                fieldWithPath("data[].registrationStartDate").type(JsonFieldType.STRING).description("접수 시작일"),
                                fieldWithPath("data[].registrationEndDate").type(JsonFieldType.STRING).description("접수 마감일"),
                                fieldWithPath("data[].examStartDate").type(JsonFieldType.STRING).description("시험 시작일"),
                                fieldWithPath("data[].examEndDate").type(JsonFieldType.STRING).description("시험 종료일")
                        )
                )));
    }

    @Test
    void 대학생활_사이트_조회() throws Exception {
        when(homeQueryService.getUsefulSites()).thenReturn(List.of(
                new UsefulSiteResponse("에브리타임", "학교 생활과 시간표", "https://everytime.kr")
        ));

        mockMvc.perform(get("/api/v1/home/useful-sites").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("에브리타임"))
                .andDo(document("home/get-useful-sites",
                        responseFields(fields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("대학생활 사이트"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("사이트 이름"),
                                fieldWithPath("data[].description").type(JsonFieldType.STRING).description("사이트 설명"),
                                fieldWithPath("data[].url").type(JsonFieldType.STRING).description("이동 URL")
                        )
                )));
    }

    private List<RecentCommentedPostResponse> recentPosts() {
        return List.of(new RecentCommentedPostResponse(
                20L, "최근 대화", LocalDateTime.of(2026, 8, 3, 12, 0)
        ));
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] recentPostFields() {
        return fields(
                fieldWithPath("data").type(JsonFieldType.ARRAY).description("최근 댓글이 달린 게시글"),
                fieldWithPath("data[].postId").type(JsonFieldType.NUMBER).description("게시글 ID"),
                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("게시글 제목"),
                fieldWithPath("data[].lastCommentedAt").type(JsonFieldType.STRING).description("마지막 댓글 시각")
        );
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] commonFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
        };
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] fields(
            org.springframework.restdocs.payload.FieldDescriptor... dataFields
    ) {
        List<org.springframework.restdocs.payload.FieldDescriptor> fields = new ArrayList<>(
                List.of(commonFields())
        );
        fields.addAll(List.of(dataFields));
        return fields.toArray(org.springframework.restdocs.payload.FieldDescriptor[]::new);
    }

    private MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }
}
