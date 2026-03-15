package cluverse.group.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupStatus;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.service.GroupService;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupInterestResponse;
import cluverse.group.service.response.GroupRoleResponse;
import cluverse.member.domain.MemberRole;
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

class GroupControllerDocsTest extends RestDocsSupport {

    private final GroupService groupService = mock(GroupService.class);

    @Override
    protected Object initController() {
        return new GroupController(groupService);
    }

    @Test
    void 그룹_생성() throws Exception {
        // given
        when(groupService.createGroup(eq(1L), any())).thenReturn(new GroupDetailResponse(
                1L,
                11L,
                "AI 프로젝트",
                "함께 만드는 AI 프로젝트 그룹",
                "https://cdn.example.com/group.png",
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                GroupStatus.ACTIVE,
                1L,
                "luna",
                10,
                1,
                true,
                GroupMemberRole.OWNER,
                1L,
                List.of(new GroupInterestResponse(1L, "인공지능")),
                List.of(new GroupRoleResponse(1L, "운영진", 1)),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 10, 0)
        ));

        // when, then
        mockMvc.perform(post("/api/v1/groups")
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "AI 프로젝트",
                                    "description": "함께 만드는 AI 프로젝트 그룹",
                                    "coverImageUrl": "https://cdn.example.com/group.png",
                                    "category": "PROJECT",
                                    "activityType": "HYBRID",
                                    "region": "서울",
                                    "visibility": "PUBLIC",
                                    "maxMembers": 10,
                                    "interestIds": [1, 2]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupId").value(1))
                .andDo(document("groups/create",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("그룹명"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("그룹 소개").optional(),
                                fieldWithPath("coverImageUrl").type(JsonFieldType.STRING).description("대표 이미지 URL").optional(),
                                fieldWithPath("category").type(JsonFieldType.STRING).description("그룹 카테고리"),
                                fieldWithPath("activityType").type(JsonFieldType.STRING).description("활동 방식"),
                                fieldWithPath("region").type(JsonFieldType.STRING).description("활동 지역").optional(),
                                fieldWithPath("visibility").type(JsonFieldType.STRING).description("공개 범위"),
                                fieldWithPath("maxMembers").type(JsonFieldType.NUMBER).description("최대 인원").optional(),
                                fieldWithPath("interestIds").type(JsonFieldType.ARRAY).description("관심 태그 ID 목록").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                                fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("그룹 전용 보드 ID"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("그룹명"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("그룹 소개").optional(),
                                fieldWithPath("data.coverImageUrl").type(JsonFieldType.STRING).description("대표 이미지 URL").optional(),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("그룹 카테고리"),
                                fieldWithPath("data.activityType").type(JsonFieldType.STRING).description("활동 방식"),
                                fieldWithPath("data.region").type(JsonFieldType.STRING).description("활동 지역").optional(),
                                fieldWithPath("data.visibility").type(JsonFieldType.STRING).description("공개 범위"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("그룹 상태"),
                                fieldWithPath("data.ownerId").type(JsonFieldType.NUMBER).description("오너 회원 ID"),
                                fieldWithPath("data.ownerNickname").type(JsonFieldType.STRING).description("오너 닉네임").optional(),
                                fieldWithPath("data.maxMembers").type(JsonFieldType.NUMBER).description("최대 인원").optional(),
                                fieldWithPath("data.memberCount").type(JsonFieldType.NUMBER).description("현재 멤버 수"),
                                fieldWithPath("data.member").type(JsonFieldType.BOOLEAN).description("내 멤버 여부"),
                                fieldWithPath("data.myRole").type(JsonFieldType.STRING).description("내 그룹 역할").optional(),
                                fieldWithPath("data.openRecruitmentCount").type(JsonFieldType.NUMBER).description("오픈된 모집글 수"),
                                fieldWithPath("data.interests").type(JsonFieldType.ARRAY).description("관심 태그 목록"),
                                fieldWithPath("data.interests[].interestId").type(JsonFieldType.NUMBER).description("관심 태그 ID"),
                                fieldWithPath("data.interests[].name").type(JsonFieldType.STRING).description("관심 태그명").optional(),
                                fieldWithPath("data.roles").type(JsonFieldType.ARRAY).description("그룹 직책 목록"),
                                fieldWithPath("data.roles[].groupRoleId").type(JsonFieldType.NUMBER).description("그룹 직책 ID"),
                                fieldWithPath("data.roles[].title").type(JsonFieldType.STRING).description("직책명"),
                                fieldWithPath("data.roles[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서"),
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
