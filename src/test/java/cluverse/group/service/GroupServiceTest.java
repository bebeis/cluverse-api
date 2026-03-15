package cluverse.group.service;

import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.service.implement.GroupReader;
import cluverse.group.service.implement.GroupWriter;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
import cluverse.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupReader groupReader;

    @Mock
    private GroupWriter groupWriter;

    @InjectMocks
    private GroupService groupService;

    @Test
    void 그룹_생성시_상세_응답을_반환한다() {
        // given
        GroupCreateRequest request = new GroupCreateRequest(
                "AI 프로젝트",
                "함께 만드는 AI 프로젝트 그룹",
                "https://cdn.example.com/group.png",
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                10,
                List.of(1L, 2L)
        );
        Group group = createGroup(1L, 11L, 100L);
        Member owner = createMember(100L, "luna");

        when(groupWriter.create(100L, request)).thenReturn(group);
        when(groupReader.readOrThrow(1L)).thenReturn(group);
        when(groupReader.readMemberMap(List.of(100L))).thenReturn(Map.of(100L, owner));
        when(groupReader.readInterestMap(List.of(1L, 2L))).thenReturn(Map.of());
        when(groupReader.countOpenRecruitments(1L)).thenReturn(0L);

        // when
        GroupDetailResponse result = groupService.createGroup(100L, request);

        // then
        assertThat(result.groupId()).isEqualTo(1L);
        assertThat(result.ownerNickname()).isEqualTo("luna");
        assertThat(result.member()).isTrue();
        verify(groupWriter).create(100L, request);
        verify(groupReader).readOrThrow(1L);
    }

    @Test
    void 그룹_수정은_매니저만_가능하다() {
        // given
        Group group = createGroup(1L, 11L, 100L);
        GroupUpdateRequest request = new GroupUpdateRequest(
                "AI 프로젝트 리뉴얼",
                "소개 변경",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.ONLINE,
                null,
                GroupVisibility.PARTIAL,
                20,
                List.of()
        );
        when(groupReader.readOrThrow(1L)).thenReturn(group);

        // when, then
        assertThatThrownBy(() -> groupService.updateGroup(200L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("그룹 관리 권한이 없습니다.");
    }

    private Group createGroup(Long groupId, Long boardId, Long ownerId) {
        Group group = Group.create(
                boardId,
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                ownerId,
                10,
                List.of(1L, 2L)
        );
        ReflectionTestUtils.setField(group, "id", groupId);
        return group;
    }

    private Member createMember(Long memberId, String nickname) {
        Member member = Member.createSocialMember(nickname);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
