package cluverse.group.controller;

import cluverse.common.auth.LoginMember;
import cluverse.docs.RestDocsSupport;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupStatus;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.service.GroupService;
import cluverse.group.service.request.GroupMemberUpdateRequest;
import cluverse.group.service.request.GroupOwnerTransferRequest;
import cluverse.group.service.request.GroupRoleCreateRequest;
import cluverse.group.service.request.GroupRoleUpdateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.group.service.response.GroupInterestResponse;
import cluverse.group.service.response.GroupMemberResponse;
import cluverse.group.service.response.GroupPageResponse;
import cluverse.group.service.response.GroupRoleResponse;
import cluverse.group.service.response.GroupSummaryResponse;
import cluverse.group.service.response.MyGroupSummaryResponse;
import cluverse.member.domain.MemberRole;
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

class GroupControllerDocsTest extends RestDocsSupport {

    private final GroupService groupService = mock(GroupService.class);

    @Override
    protected Object initController() {
        return new GroupController(groupService);
    }

    @Test
    void 그룹_목록_조회() throws Exception {
        // given
        when(groupService.getGroups(eq(1L), any())).thenReturn(new GroupPageResponse(
                List.of(createGroupSummaryResponse()),
                1,
                20,
                false
        ));

        // when, then
        mockMvc.perform(get("/api/v1/groups")
                        .session(createMemberSession())
                        .queryParam("keyword", "AI")
                        .queryParam("category", "PROJECT")
                        .queryParam("activityType", "HYBRID")
                        .queryParam("region", "서울")
                        .queryParam("visibility", "PUBLIC")
                        .queryParam("recruitableOnly", "true")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].groupId").value(1))
                .andDo(document("groups/get-group-list",
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드").optional(),
                                parameterWithName("category").description("그룹 카테고리").optional(),
                                parameterWithName("activityType").description("활동 방식").optional(),
                                parameterWithName("region").description("활동 지역").optional(),
                                parameterWithName("visibility").description("공개 범위").optional(),
                                parameterWithName("recruitableOnly").description("모집 중인 그룹만 조회할지 여부").optional(),
                                parameterWithName("page").description("페이지 번호").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(groupPageResponseFields())
                ));
    }

    @Test
    void 내_그룹_목록_조회() throws Exception {
        // given
        when(groupService.getMyGroups(1L)).thenReturn(List.of(
                new MyGroupSummaryResponse(
                        1L,
                        "AI 프로젝트",
                        GroupCategory.PROJECT,
                        GroupVisibility.PUBLIC,
                        GroupMemberRole.OWNER,
                        8,
                        1L
                )
        ));

        // when, then
        mockMvc.perform(get("/api/v1/groups/me")
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupId").value(1))
                .andDo(document("groups/get-my-groups",
                        responseFields(myGroupResponseFields())
                ));
    }

    @Test
    void 그룹_생성() throws Exception {
        // given
        when(groupService.createGroup(eq(1L), any())).thenReturn(createGroupDetailResponse());

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
                        responseFields(groupDetailResponseFields())
                ));
    }

    @Test
    void 그룹_상세_조회() throws Exception {
        // given
        when(groupService.getGroup(1L, 1L)).thenReturn(createGroupDetailResponse());

        // when, then
        mockMvc.perform(get("/api/v1/groups/{groupId}", 1L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(1))
                .andDo(document("groups/get-group",
                        pathParameters(
                                parameterWithName("groupId").description("조회할 그룹 ID")
                        ),
                        responseFields(groupDetailResponseFields())
                ));
    }

    @Test
    void 그룹_수정() throws Exception {
        // given
        when(groupService.updateGroup(eq(1L), eq(1L), any(GroupUpdateRequest.class)))
                .thenReturn(createUpdatedGroupDetailResponse());

        // when, then
        mockMvc.perform(put("/api/v1/groups/{groupId}", 1L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "AI 프로젝트 시즌2",
                                    "description": "함께 만드는 AI 프로젝트 그룹 시즌2",
                                    "coverImageUrl": "https://cdn.example.com/group-v2.png",
                                    "category": "PROJECT",
                                    "activityType": "ONLINE",
                                    "region": "전국",
                                    "visibility": "PUBLIC",
                                    "maxMembers": 12,
                                    "interestIds": [1, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("AI 프로젝트 시즌2"))
                .andDo(document("groups/update-group",
                        pathParameters(
                                parameterWithName("groupId").description("수정할 그룹 ID")
                        ),
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
                        responseFields(groupDetailResponseFields())
                ));
    }

    @Test
    void 그룹_멤버_목록_조회() throws Exception {
        // given
        when(groupService.getMembers(1L, 1L)).thenReturn(List.of(
                new GroupMemberResponse(
                        1L,
                        "luna",
                        "https://cdn.example.com/profile.png",
                        GroupMemberRole.OWNER,
                        1L,
                        "운영진",
                        LocalDateTime.of(2026, 3, 16, 10, 0),
                        true
                )
        ));

        // when, then
        mockMvc.perform(get("/api/v1/groups/{groupId}/members", 1L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(1))
                .andDo(document("groups/get-group-members",
                        pathParameters(
                                parameterWithName("groupId").description("멤버를 조회할 그룹 ID")
                        ),
                        responseFields(groupMemberResponseFields())
                ));
    }

    @Test
    void 그룹_멤버_권한_수정() throws Exception {
        // given
        when(groupService.updateMember(eq(1L), eq(1L), eq(2L), any(GroupMemberUpdateRequest.class), eq("127.0.0.1")))
                .thenReturn(new GroupMemberResponse(
                        2L,
                        "nova",
                        "https://cdn.example.com/nova.png",
                        GroupMemberRole.ADMIN,
                        1L,
                        "운영진",
                        LocalDateTime.of(2026, 3, 15, 10, 0),
                        false
                ));

        // when, then
        mockMvc.perform(patch("/api/v1/groups/{groupId}/members/{memberId}", 1L, 2L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "ADMIN",
                                    "customTitleId": 1,
                                    "reason": "운영 권한 부여"
                                }
                                """)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andDo(document("groups/update-group-member",
                        pathParameters(
                                parameterWithName("groupId").description("그룹 ID"),
                                parameterWithName("memberId").description("권한을 수정할 회원 ID")
                        ),
                        requestFields(
                                fieldWithPath("role").type(JsonFieldType.STRING).description("변경할 그룹 역할"),
                                fieldWithPath("customTitleId").type(JsonFieldType.NUMBER).description("부여할 커스텀 직책 ID").optional(),
                                fieldWithPath("reason").type(JsonFieldType.STRING).description("변경 사유").optional()
                        ),
                        responseFields(groupMemberSingleResponseFields())
                ));
    }

    @Test
    void 그룹_탈퇴() throws Exception {
        // given
        doNothing().when(groupService).leaveGroup(1L, 1L, "127.0.0.1");

        // when, then
        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", 1L)
                        .session(createMemberSession())
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andDo(document("groups/leave-group",
                        pathParameters(
                                parameterWithName("groupId").description("탈퇴할 그룹 ID")
                        ),
                        responseFields(voidResponseFields())
                ));
    }

    @Test
    void 그룹_멤버_제거() throws Exception {
        // given
        doNothing().when(groupService).removeMember(1L, 1L, 2L, "127.0.0.1");

        // when, then
        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{memberId}", 1L, 2L)
                        .session(createMemberSession())
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andDo(document("groups/remove-group-member",
                        pathParameters(
                                parameterWithName("groupId").description("그룹 ID"),
                                parameterWithName("memberId").description("제거할 회원 ID")
                        ),
                        responseFields(voidResponseFields())
                ));
    }

    @Test
    void 그룹_오너_이관() throws Exception {
        // given
        when(groupService.transferOwner(eq(1L), eq(1L), any(GroupOwnerTransferRequest.class), eq("127.0.0.1")))
                .thenReturn(createTransferredOwnerGroupDetailResponse());

        // when, then
        mockMvc.perform(post("/api/v1/groups/{groupId}/owner-transfer", 1L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "newOwnerMemberId": 2,
                                    "reason": "운영 이관"
                                }
                                """)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(2))
                .andDo(document("groups/transfer-group-owner",
                        pathParameters(
                                parameterWithName("groupId").description("오너를 이관할 그룹 ID")
                        ),
                        requestFields(
                                fieldWithPath("newOwnerMemberId").type(JsonFieldType.NUMBER).description("새 오너 회원 ID"),
                                fieldWithPath("reason").type(JsonFieldType.STRING).description("이관 사유").optional()
                        ),
                        responseFields(groupDetailResponseFields())
                ));
    }

    @Test
    void 그룹_직책_목록_조회() throws Exception {
        // given
        when(groupService.getRoles(1L, 1L)).thenReturn(List.of(
                new GroupRoleResponse(1L, "운영진", 1),
                new GroupRoleResponse(2L, "디자이너", 2)
        ));

        // when, then
        mockMvc.perform(get("/api/v1/groups/{groupId}/roles", 1L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupRoleId").value(1))
                .andDo(document("groups/get-group-roles",
                        pathParameters(
                                parameterWithName("groupId").description("직책을 조회할 그룹 ID")
                        ),
                        responseFields(groupRoleListResponseFields())
                ));
    }

    @Test
    void 그룹_직책_생성() throws Exception {
        // given
        when(groupService.createRole(eq(1L), eq(1L), any(GroupRoleCreateRequest.class)))
                .thenReturn(new GroupRoleResponse(3L, "백엔드 리드", 3));

        // when, then
        mockMvc.perform(post("/api/v1/groups/{groupId}/roles", 1L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "백엔드 리드",
                                    "displayOrder": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupRoleId").value(3))
                .andDo(document("groups/create-group-role",
                        pathParameters(
                                parameterWithName("groupId").description("직책을 생성할 그룹 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("직책명"),
                                fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("표시 순서").optional()
                        ),
                        responseFields(groupRoleSingleResponseFields())
                ));
    }

    @Test
    void 그룹_직책_수정() throws Exception {
        // given
        when(groupService.updateRole(eq(1L), eq(1L), eq(3L), any(GroupRoleUpdateRequest.class)))
                .thenReturn(new GroupRoleResponse(3L, "플랫폼 리드", 1));

        // when, then
        mockMvc.perform(put("/api/v1/groups/{groupId}/roles/{roleId}", 1L, 3L)
                        .session(createMemberSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "플랫폼 리드",
                                    "displayOrder": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("플랫폼 리드"))
                .andDo(document("groups/update-group-role",
                        pathParameters(
                                parameterWithName("groupId").description("그룹 ID"),
                                parameterWithName("roleId").description("수정할 직책 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("직책명"),
                                fieldWithPath("displayOrder").type(JsonFieldType.NUMBER).description("표시 순서").optional()
                        ),
                        responseFields(groupRoleSingleResponseFields())
                ));
    }

    @Test
    void 그룹_직책_삭제() throws Exception {
        // given
        doNothing().when(groupService).deleteRole(1L, 1L, 3L);

        // when, then
        mockMvc.perform(delete("/api/v1/groups/{groupId}/roles/{roleId}", 1L, 3L)
                        .session(createMemberSession()))
                .andExpect(status().isOk())
                .andDo(document("groups/delete-group-role",
                        pathParameters(
                                parameterWithName("groupId").description("그룹 ID"),
                                parameterWithName("roleId").description("삭제할 직책 ID")
                        ),
                        responseFields(voidResponseFields())
                ));
    }

    private MockHttpSession createMemberSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY, new LoginMember(1L, "luna", MemberRole.MEMBER));
        return session;
    }

    private GroupSummaryResponse createGroupSummaryResponse() {
        return new GroupSummaryResponse(
                1L,
                "AI 프로젝트",
                "함께 만드는 AI 프로젝트 그룹",
                "https://cdn.example.com/group.png",
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                GroupStatus.ACTIVE,
                10,
                8,
                true,
                1L,
                List.of(new GroupInterestResponse(1L, "인공지능"))
        );
    }

    private GroupDetailResponse createGroupDetailResponse() {
        return new GroupDetailResponse(
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
                8,
                true,
                GroupMemberRole.OWNER,
                1L,
                List.of(new GroupInterestResponse(1L, "인공지능")),
                List.of(new GroupRoleResponse(1L, "운영진", 1)),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 16, 10, 0)
        );
    }

    private GroupDetailResponse createUpdatedGroupDetailResponse() {
        return new GroupDetailResponse(
                1L,
                11L,
                "AI 프로젝트 시즌2",
                "함께 만드는 AI 프로젝트 그룹 시즌2",
                "https://cdn.example.com/group-v2.png",
                GroupCategory.PROJECT,
                GroupActivityType.ONLINE,
                "전국",
                GroupVisibility.PUBLIC,
                GroupStatus.ACTIVE,
                1L,
                "luna",
                12,
                8,
                true,
                GroupMemberRole.OWNER,
                2L,
                List.of(
                        new GroupInterestResponse(1L, "인공지능"),
                        new GroupInterestResponse(3L, "백엔드")
                ),
                List.of(new GroupRoleResponse(1L, "운영진", 1)),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 17, 10, 0)
        );
    }

    private GroupDetailResponse createTransferredOwnerGroupDetailResponse() {
        return new GroupDetailResponse(
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
                2L,
                "nova",
                10,
                8,
                true,
                GroupMemberRole.ADMIN,
                1L,
                List.of(new GroupInterestResponse(1L, "인공지능")),
                List.of(new GroupRoleResponse(1L, "운영진", 1)),
                LocalDateTime.of(2026, 3, 16, 10, 0),
                LocalDateTime.of(2026, 3, 17, 12, 0)
        );
    }

    private FieldDescriptor[] groupPageResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.groups").type(JsonFieldType.ARRAY).description("그룹 목록"),
                fieldWithPath("data.groups[].groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("data.groups[].name").type(JsonFieldType.STRING).description("그룹명"),
                fieldWithPath("data.groups[].description").type(JsonFieldType.STRING).description("그룹 소개").optional(),
                fieldWithPath("data.groups[].coverImageUrl").type(JsonFieldType.STRING).description("대표 이미지 URL").optional(),
                fieldWithPath("data.groups[].category").type(JsonFieldType.STRING).description("그룹 카테고리"),
                fieldWithPath("data.groups[].activityType").type(JsonFieldType.STRING).description("활동 방식"),
                fieldWithPath("data.groups[].region").type(JsonFieldType.STRING).description("활동 지역").optional(),
                fieldWithPath("data.groups[].visibility").type(JsonFieldType.STRING).description("공개 범위"),
                fieldWithPath("data.groups[].status").type(JsonFieldType.STRING).description("그룹 상태"),
                fieldWithPath("data.groups[].maxMembers").type(JsonFieldType.NUMBER).description("최대 인원").optional(),
                fieldWithPath("data.groups[].memberCount").type(JsonFieldType.NUMBER).description("현재 멤버 수"),
                fieldWithPath("data.groups[].recruiting").type(JsonFieldType.BOOLEAN).description("모집 중 여부"),
                fieldWithPath("data.groups[].openRecruitmentCount").type(JsonFieldType.NUMBER).description("오픈된 모집글 수"),
                fieldWithPath("data.groups[].interests").type(JsonFieldType.ARRAY).description("관심 태그 목록"),
                fieldWithPath("data.groups[].interests[].interestId").type(JsonFieldType.NUMBER).description("관심 태그 ID"),
                fieldWithPath("data.groups[].interests[].name").type(JsonFieldType.STRING).description("관심 태그명").optional(),
                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
        };
    }

    private FieldDescriptor[] myGroupResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data[].groupId").type(JsonFieldType.NUMBER).description("그룹 ID"),
                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("그룹명"),
                fieldWithPath("data[].category").type(JsonFieldType.STRING).description("그룹 카테고리"),
                fieldWithPath("data[].visibility").type(JsonFieldType.STRING).description("공개 범위"),
                fieldWithPath("data[].myRole").type(JsonFieldType.STRING).description("내 그룹 역할"),
                fieldWithPath("data[].memberCount").type(JsonFieldType.NUMBER).description("현재 멤버 수"),
                fieldWithPath("data[].openRecruitmentCount").type(JsonFieldType.NUMBER).description("오픈된 모집글 수")
        };
    }

    private FieldDescriptor[] groupDetailResponseFields() {
        return new FieldDescriptor[]{
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
        };
    }

    private FieldDescriptor[] groupMemberResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data[].memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("회원 닉네임").optional(),
                fieldWithPath("data[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("data[].role").type(JsonFieldType.STRING).description("그룹 역할"),
                fieldWithPath("data[].customTitleId").type(JsonFieldType.NUMBER).description("커스텀 직책 ID").optional(),
                fieldWithPath("data[].customTitle").type(JsonFieldType.STRING).description("커스텀 직책명").optional(),
                fieldWithPath("data[].joinedAt").type(JsonFieldType.STRING).description("가입 일시"),
                fieldWithPath("data[].isMe").type(JsonFieldType.BOOLEAN).description("내 계정 여부")
        };
    }

    private FieldDescriptor[] groupMemberSingleResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("회원 닉네임").optional(),
                fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL").optional(),
                fieldWithPath("data.role").type(JsonFieldType.STRING).description("그룹 역할"),
                fieldWithPath("data.customTitleId").type(JsonFieldType.NUMBER).description("커스텀 직책 ID").optional(),
                fieldWithPath("data.customTitle").type(JsonFieldType.STRING).description("커스텀 직책명").optional(),
                fieldWithPath("data.joinedAt").type(JsonFieldType.STRING).description("가입 일시"),
                fieldWithPath("data.isMe").type(JsonFieldType.BOOLEAN).description("내 계정 여부")
        };
    }

    private FieldDescriptor[] groupRoleListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data[].groupRoleId").type(JsonFieldType.NUMBER).description("그룹 직책 ID"),
                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("직책명"),
                fieldWithPath("data[].displayOrder").type(JsonFieldType.NUMBER).description("표시 순서")
        };
    }

    private FieldDescriptor[] groupRoleSingleResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                fieldWithPath("data.groupRoleId").type(JsonFieldType.NUMBER).description("그룹 직책 ID"),
                fieldWithPath("data.title").type(JsonFieldType.STRING).description("직책명"),
                fieldWithPath("data.displayOrder").type(JsonFieldType.NUMBER).description("표시 순서")
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
