package cluverse.group.service;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.service.implement.BoardReader;
import cluverse.board.service.implement.BoardWriter;
import cluverse.common.exception.ForbiddenException;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.repository.dto.GroupMemberSummaryQueryDto;
import cluverse.group.service.implement.GroupReader;
import cluverse.group.service.implement.GroupWriter;
import cluverse.group.service.request.GroupCreateRequest;
import cluverse.group.service.request.GroupUpdateRequest;
import cluverse.group.service.response.GroupDetailResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupReader groupReader;

    @Mock
    private GroupWriter groupWriter;

    @Mock
    private BoardReader boardReader;

    @Mock
    private BoardWriter boardWriter;

    @InjectMocks
    private GroupQueryService groupQueryService;

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
        Board board = createBoard(11L, "AI 프로젝트");
        GroupMemberSummaryQueryDto owner = createMemberSummary(100L, "luna");

        when(boardWriter.createGroupBoard("AI 프로젝트", "함께 만드는 AI 프로젝트 그룹")).thenReturn(board);
        when(groupWriter.create(100L, 11L, request)).thenReturn(group);
        when(groupReader.readActiveOrThrow(1L)).thenReturn(group);
        when(groupReader.readMemberSummaryMap(List.of(100L))).thenReturn(Map.of(100L, owner));
        when(groupReader.readInterestNameMap(List.of(1L, 2L))).thenReturn(Map.of());
        when(groupReader.countOpenRecruitments(List.of(1L))).thenReturn(Map.of(1L, 0L));

        // when
        GroupDetailResponse result = groupService.createGroup(100L, request);

        // then
        assertThat(result.groupId()).isEqualTo(1L);
        assertThat(result.ownerNickname()).isEqualTo("luna");
        assertThat(result.member()).isTrue();
        verify(boardWriter).createGroupBoard("AI 프로젝트", "함께 만드는 AI 프로젝트 그룹");
        verify(groupWriter).create(100L, 11L, request);
        verify(groupReader).readActiveOrThrow(1L);
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
        when(groupReader.readActiveOrThrow(1L)).thenReturn(group);

        // when, then
        assertThatThrownBy(() -> groupService.updateGroup(200L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("그룹 관리 권한이 없습니다.");
        verifyNoMoreInteractions(boardWriter, boardReader);
    }

    @Test
    void 그룹_수정시_연결된_보드_이름과_설명도_함께_갱신한다() {
        // given
        Group group = createGroup(1L, 11L, 100L);
        GroupUpdateRequest request = new GroupUpdateRequest(
                "AI 프로젝트 시즌2",
                "업데이트된 소개",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.ONLINE,
                "전국",
                GroupVisibility.PUBLIC,
                20,
                List.of(1L)
        );
        GroupMemberSummaryQueryDto owner = createMemberSummary(100L, "luna");

        when(groupReader.readActiveOrThrow(1L)).thenReturn(group);
        when(boardReader.readOrThrow(11L)).thenReturn(createBoard(11L, "AI 프로젝트"));
        when(groupReader.readMemberSummaryMap(List.of(100L))).thenReturn(Map.of(100L, owner));
        when(groupReader.readInterestNameMap(List.of(1L))).thenReturn(Map.of());
        when(groupReader.countOpenRecruitments(List.of(1L))).thenReturn(Map.of(1L, 0L));
        doAnswer(invocation -> {
            Group targetGroup = invocation.getArgument(0);
            GroupUpdateRequest updateRequest = invocation.getArgument(1);
            targetGroup.update(
                    updateRequest.name(),
                    updateRequest.description(),
                    updateRequest.coverImageUrl(),
                    updateRequest.category(),
                    updateRequest.activityType(),
                    updateRequest.region(),
                    updateRequest.visibility(),
                    updateRequest.maxMembers(),
                    updateRequest.interestIds()
            );
            return null;
        }).when(groupWriter).update(any(Group.class), any(GroupUpdateRequest.class));

        // when
        GroupDetailResponse result = groupService.updateGroup(100L, 1L, request);

        // then
        assertThat(result.name()).isEqualTo("AI 프로젝트 시즌2");
        verify(groupWriter).update(group, request);
        verify(boardWriter).updateGroupBoard(any(Board.class), org.mockito.ArgumentMatchers.eq("AI 프로젝트 시즌2"), org.mockito.ArgumentMatchers.eq("업데이트된 소개"));
    }

    @Test
    void 그룹_삭제시_그룹은_종료되고_보드도_비활성화한다() {
        // given
        Group group = createGroup(1L, 11L, 100L);
        when(groupReader.readActiveOrThrow(1L)).thenReturn(group);
        when(boardReader.readOrThrow(11L)).thenReturn(createBoard(11L, "AI 프로젝트"));
        doAnswer(invocation -> {
            Group targetGroup = invocation.getArgument(0);
            targetGroup.close();
            return null;
        }).when(groupWriter).close(any(Group.class));

        // when
        groupService.deleteGroup(100L, 1L);

        // then
        assertThat(group.getStatus()).isEqualTo(cluverse.group.domain.GroupStatus.CLOSED);
        verify(groupWriter).close(group);
        verify(boardWriter).deactivateGroupBoard(any(Board.class));
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

    private GroupMemberSummaryQueryDto createMemberSummary(Long memberId, String nickname) {
        return new GroupMemberSummaryQueryDto(memberId, nickname, null);
    }

    private Board createBoard(Long boardId, String name) {
        Board board = Board.create(BoardType.GROUP, name, "설명", null, 0, 0, true);
        ReflectionTestUtils.setField(board, "id", boardId);
        return board;
    }
}
