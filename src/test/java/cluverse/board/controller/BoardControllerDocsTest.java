package cluverse.board.controller;

import cluverse.board.domain.BoardType;
import cluverse.board.service.BoardService;
import cluverse.board.service.response.BoardBreadcrumbResponse;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import cluverse.board.service.response.BoardHomeTabResponse;
import cluverse.board.service.response.BoardHomeTabType;
import cluverse.board.service.response.BoardPostingPolicyResponse;
import cluverse.board.service.response.BoardSortOption;
import cluverse.board.service.response.BoardSummaryResponse;
import cluverse.board.service.response.GroupBoardSummaryResponse;
import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupVisibility;
import cluverse.member.domain.MemberRole;
import cluverse.post.domain.PostCategory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static cluverse.common.auth.LoginMemberArgumentResolver.SESSION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BoardControllerDocsTest extends RestDocsSupport {

    private final BoardService boardService = mock(BoardService.class);

    @Override
    protected Object initController() {
        return new BoardController(boardService);
    }

    @Test
    void 보드_디렉토리_조회() throws Exception {
        when(boardService.getBoardDirectory(anyLong(), any())).thenReturn(new BoardDirectoryResponse(
                BoardType.DEPARTMENT,
                10L,
                2,
                true,
                List.of(
                        new BoardSummaryResponse(
                                11L,
                                BoardType.DEPARTMENT,
                                "소프트웨어공학",
                                "컴퓨터공학 하위 세부 전공 보드",
                                10L,
                                1,
                                1,
                                true,
                                0L,
                                true,
                                true,
                                false
                        ),
                        new BoardSummaryResponse(
                                12L,
                                BoardType.DEPARTMENT,
                                "인공지능",
                                "컴퓨터공학 하위 AI 보드",
                                10L,
                                1,
                                2,
                                true,
                                0L,
                                true,
                                true,
                                false
                        )
                )
        ));

        mockMvc.perform(get("/api/v1/boards")
                        .session(createMemberSession())
                        .queryParam("type", "DEPARTMENT")
                        .queryParam("depth", "2")
                        .queryParam("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boards[0].boardId").value(11))
                .andDo(document("boards/get-directory",
                        queryParameters(
                                parameterWithName("type").description("보드 타입 (`DEPARTMENT`, `INTEREST`, `GROUP`)").optional(),
                                parameterWithName("keyword").description("보드명 검색어").optional(),
                                parameterWithName("parentBoardId").description("하위 보드를 조회할 부모 보드 ID").optional(),
                                parameterWithName("depth").description("탐색 깊이 (0~2)").optional(),
                                parameterWithName("activeOnly").description("활성 보드만 조회할지 여부").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.boardType").type(JsonFieldType.STRING).description("조회 기준 보드 타입").optional(),
                                fieldWithPath("data.parentBoardId").type(JsonFieldType.NUMBER).description("조회 기준 부모 보드 ID").optional(),
                                fieldWithPath("data.requestedDepth").type(JsonFieldType.NUMBER).description("요청된 탐색 깊이"),
                                fieldWithPath("data.activeOnly").type(JsonFieldType.BOOLEAN).description("활성 보드만 조회했는지 여부"),
                                fieldWithPath("data.boards").type(JsonFieldType.ARRAY).description("보드 디렉토리 목록"),
                                fieldWithPath("data.boards[].boardId").type(JsonFieldType.NUMBER).description("보드 ID"),
                                fieldWithPath("data.boards[].boardType").type(JsonFieldType.STRING).description("보드 타입"),
                                fieldWithPath("data.boards[].name").type(JsonFieldType.STRING).description("보드명"),
                                fieldWithPath("data.boards[].description").type(JsonFieldType.STRING).description("보드 설명").optional(),
                                fieldWithPath("data.boards[].parentBoardId").type(JsonFieldType.NUMBER).description("부모 보드 ID").optional(),
                                fieldWithPath("data.boards[].depth").type(JsonFieldType.NUMBER).description("보드 depth"),
                                fieldWithPath("data.boards[].displayOrder").type(JsonFieldType.NUMBER).description("노출 순서"),
                                fieldWithPath("data.boards[].isActive").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                                fieldWithPath("data.boards[].childCount").type(JsonFieldType.NUMBER).description("직계 하위 보드 수"),
                                fieldWithPath("data.boards[].isReadable").type(JsonFieldType.BOOLEAN).description("조회 가능 여부"),
                                fieldWithPath("data.boards[].isWritable").type(JsonFieldType.BOOLEAN).description("작성 가능 여부"),
                                fieldWithPath("data.boards[].isMemberOnly").type(JsonFieldType.BOOLEAN).description("멤버 전용 보드 여부")
                        )
                ));
    }

    @Test
    void 보드_상세_조회() throws Exception {
        when(boardService.getBoard(anyLong(), anyLong())).thenReturn(createBoardDetailResponse());

        mockMvc.perform(get("/api/v1/boards/{boardId}", 31L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardId").value(31))
                .andDo(document("boards/get-detail",
                        pathParameters(
                                parameterWithName("boardId").description("조회할 보드 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("보드 ID"),
                                fieldWithPath("data.boardType").type(JsonFieldType.STRING).description("보드 타입"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("보드명"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("보드 설명").optional(),
                                fieldWithPath("data.parentBoardId").type(JsonFieldType.NULL).description("부모 보드 ID").optional(),
                                fieldWithPath("data.depth").type(JsonFieldType.NUMBER).description("보드 depth"),
                                fieldWithPath("data.displayOrder").type(JsonFieldType.NUMBER).description("노출 순서"),
                                fieldWithPath("data.isActive").type(JsonFieldType.BOOLEAN).description("활성 여부"),
                                fieldWithPath("data.isReadable").type(JsonFieldType.BOOLEAN).description("조회 가능 여부"),
                                fieldWithPath("data.isWritable").type(JsonFieldType.BOOLEAN).description("작성 가능 여부"),
                                fieldWithPath("data.isManageable").type(JsonFieldType.BOOLEAN).description("관리 가능 여부"),
                                fieldWithPath("data.isMemberOnly").type(JsonFieldType.BOOLEAN).description("멤버 전용 보드 여부"),
                                fieldWithPath("data.postingPolicy").type(JsonFieldType.OBJECT).description("게시 정책"),
                                fieldWithPath("data.postingPolicy.isAnonymousAllowed").type(JsonFieldType.BOOLEAN).description("익명 작성 허용 여부"),
                                fieldWithPath("data.postingPolicy.isExternalVisibleAllowed").type(JsonFieldType.BOOLEAN).description("외부 공개 허용 여부"),
                                fieldWithPath("data.postingPolicy.isPinnedAllowed").type(JsonFieldType.BOOLEAN).description("상단 고정 허용 여부"),
                                fieldWithPath("data.postingPolicy.externalVisibilityRule").type(JsonFieldType.STRING).description("외부 공개 규칙"),
                                fieldWithPath("data.postingPolicy.writePermissionRule").type(JsonFieldType.STRING).description("작성 권한 규칙"),
                                fieldWithPath("data.postingPolicy.supportedCategories").type(JsonFieldType.ARRAY).description("지원 게시글 카테고리"),
                                fieldWithPath("data.breadcrumbs").type(JsonFieldType.ARRAY).description("보드 경로"),
                                fieldWithPath("data.breadcrumbs[].boardId").type(JsonFieldType.NUMBER).description("경로 보드 ID"),
                                fieldWithPath("data.breadcrumbs[].name").type(JsonFieldType.STRING).description("경로 보드명"),
                                fieldWithPath("data.breadcrumbs[].depth").type(JsonFieldType.NUMBER).description("경로 depth"),
                                fieldWithPath("data.children").type(JsonFieldType.ARRAY).description("직계 하위 보드 목록"),
                                fieldWithPath("data.children[].boardId").type(JsonFieldType.NUMBER).description("하위 보드 ID"),
                                fieldWithPath("data.children[].boardType").type(JsonFieldType.STRING).description("하위 보드 타입"),
                                fieldWithPath("data.children[].name").type(JsonFieldType.STRING).description("하위 보드명"),
                                fieldWithPath("data.children[].description").type(JsonFieldType.STRING).description("하위 보드 설명").optional(),
                                fieldWithPath("data.children[].parentBoardId").type(JsonFieldType.NUMBER).description("하위 보드의 부모 보드 ID").optional(),
                                fieldWithPath("data.children[].depth").type(JsonFieldType.NUMBER).description("하위 보드 depth"),
                                fieldWithPath("data.children[].displayOrder").type(JsonFieldType.NUMBER).description("하위 보드 노출 순서"),
                                fieldWithPath("data.children[].isActive").type(JsonFieldType.BOOLEAN).description("하위 보드 활성 여부"),
                                fieldWithPath("data.children[].childCount").type(JsonFieldType.NUMBER).description("하위 보드의 직계 하위 보드 수"),
                                fieldWithPath("data.children[].isReadable").type(JsonFieldType.BOOLEAN).description("하위 보드 조회 가능 여부"),
                                fieldWithPath("data.children[].isWritable").type(JsonFieldType.BOOLEAN).description("하위 보드 작성 가능 여부"),
                                fieldWithPath("data.children[].isMemberOnly").type(JsonFieldType.BOOLEAN).description("하위 보드 멤버 전용 여부"),
                                fieldWithPath("data.group").type(JsonFieldType.OBJECT).description("그룹 전용 보드인 경우 연결된 그룹 정보").optional(),
                                fieldWithPath("data.group.groupId").type(JsonFieldType.NUMBER).description("그룹 ID").optional(),
                                fieldWithPath("data.group.groupName").type(JsonFieldType.STRING).description("그룹명").optional(),
                                fieldWithPath("data.group.visibility").type(JsonFieldType.STRING).description("그룹 공개 범위").optional(),
                                fieldWithPath("data.group.isMember").type(JsonFieldType.BOOLEAN).description("내가 그룹 멤버인지 여부").optional(),
                                fieldWithPath("data.group.myRole").type(JsonFieldType.STRING).description("내 그룹 역할").optional()
                        )
                ));
    }

    @Test
    void 보드_홈_조회() throws Exception {
        when(boardService.getBoardHome(anyLong(), anyLong())).thenReturn(createBoardHomeResponse());

        mockMvc.perform(get("/api/v1/boards/{boardId}/home", 31L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultTab").value("GENERAL"))
                .andDo(document("boards/get-home",
                        pathParameters(
                                parameterWithName("boardId").description("조회할 보드 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("보드 ID"),
                                fieldWithPath("data.boardType").type(JsonFieldType.STRING).description("보드 타입"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("보드명"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("보드 설명").optional(),
                                fieldWithPath("data.isMemberOnly").type(JsonFieldType.BOOLEAN).description("멤버 전용 보드 여부"),
                                fieldWithPath("data.isReadable").type(JsonFieldType.BOOLEAN).description("조회 가능 여부"),
                                fieldWithPath("data.isWritable").type(JsonFieldType.BOOLEAN).description("작성 가능 여부"),
                                fieldWithPath("data.isManageable").type(JsonFieldType.BOOLEAN).description("관리 가능 여부"),
                                fieldWithPath("data.isExternalVisible").type(JsonFieldType.BOOLEAN).description("외부 노출 가능 여부"),
                                fieldWithPath("data.defaultTab").type(JsonFieldType.STRING).description("기본 탭"),
                                fieldWithPath("data.tabs").type(JsonFieldType.ARRAY).description("홈 탭 구성"),
                                fieldWithPath("data.tabs[].tab").type(JsonFieldType.STRING).description("탭 타입"),
                                fieldWithPath("data.tabs[].label").type(JsonFieldType.STRING).description("탭 라벨"),
                                fieldWithPath("data.tabs[].category").type(JsonFieldType.STRING).description("연결 게시글 카테고리").optional(),
                                fieldWithPath("data.tabs[].isDefault").type(JsonFieldType.BOOLEAN).description("기본 탭 여부"),
                                fieldWithPath("data.tabs[].isVisible").type(JsonFieldType.BOOLEAN).description("탭 노출 여부"),
                                fieldWithPath("data.tabs[].isWriteAllowed").type(JsonFieldType.BOOLEAN).description("탭에서 글 작성 가능 여부"),
                                fieldWithPath("data.supportedSorts").type(JsonFieldType.ARRAY).description("지원 정렬 옵션"),
                                fieldWithPath("data.postingPolicy").type(JsonFieldType.OBJECT).description("게시 정책"),
                                fieldWithPath("data.postingPolicy.isAnonymousAllowed").type(JsonFieldType.BOOLEAN).description("익명 작성 허용 여부"),
                                fieldWithPath("data.postingPolicy.isExternalVisibleAllowed").type(JsonFieldType.BOOLEAN).description("외부 공개 허용 여부"),
                                fieldWithPath("data.postingPolicy.isPinnedAllowed").type(JsonFieldType.BOOLEAN).description("상단 고정 허용 여부"),
                                fieldWithPath("data.postingPolicy.externalVisibilityRule").type(JsonFieldType.STRING).description("외부 공개 규칙"),
                                fieldWithPath("data.postingPolicy.writePermissionRule").type(JsonFieldType.STRING).description("작성 권한 규칙"),
                                fieldWithPath("data.postingPolicy.supportedCategories").type(JsonFieldType.ARRAY).description("지원 게시글 카테고리")
                        )
                ));
    }

    private MockHttpSession createMemberSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private BoardDetailResponse createBoardDetailResponse() {
        return new BoardDetailResponse(
                31L,
                BoardType.GROUP,
                "AI 프로젝트 게시판",
                "공지와 일반 글을 운영하는 그룹 전용 보드",
                null,
                0,
                0,
                true,
                true,
                true,
                true,
                true,
                createPostingPolicyResponse(),
                List.of(new BoardBreadcrumbResponse(31L, "AI 프로젝트 게시판", 0)),
                List.of(
                        new BoardSummaryResponse(
                                32L,
                                BoardType.GROUP,
                                "AI 프로젝트 자료실",
                                "후속 확장용 하위 보드",
                                31L,
                                1,
                                1,
                                true,
                                0L,
                                true,
                                true,
                                true
                        )
                ),
                new GroupBoardSummaryResponse(
                        7L,
                        "AI 프로젝트",
                        GroupVisibility.PUBLIC,
                        true,
                        GroupMemberRole.ADMIN
                )
        );
    }

    private BoardHomeResponse createBoardHomeResponse() {
        return new BoardHomeResponse(
                31L,
                BoardType.GROUP,
                "AI 프로젝트 게시판",
                "공지와 일반 글을 운영하는 그룹 전용 보드",
                true,
                true,
                true,
                true,
                true,
                BoardHomeTabType.GENERAL,
                List.of(
                        new BoardHomeTabResponse(
                                BoardHomeTabType.NOTICE,
                                "공지",
                                PostCategory.NOTICE,
                                false,
                                true,
                                true
                        ),
                        new BoardHomeTabResponse(
                                BoardHomeTabType.GENERAL,
                                "일반",
                                PostCategory.GENERAL,
                                true,
                                true,
                                true
                        )
                ),
                List.of(
                        BoardSortOption.LATEST,
                        BoardSortOption.VIEW_COUNT,
                        BoardSortOption.COMMENT_COUNT
                ),
                createPostingPolicyResponse()
        );
    }

    private BoardPostingPolicyResponse createPostingPolicyResponse() {
        return new BoardPostingPolicyResponse(
                false,
                true,
                true,
                "PUBLIC 그룹일 때만 외부 공개 가능",
                "공지 작성은 관리자, 일반 글 작성은 멤버 이상 가능",
                List.of(PostCategory.NOTICE, PostCategory.GENERAL)
        );
    }
}
