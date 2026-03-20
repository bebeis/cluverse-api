package cluverse.board.service.implement;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.common.exception.ForbiddenException;
import cluverse.board.repository.BoardQueryRepository;
import cluverse.board.repository.BoardRepository;
import cluverse.board.repository.dto.BoardGroupQueryDto;
import cluverse.board.repository.dto.BoardQueryDto;
import cluverse.board.service.request.BoardSearchRequest;
import cluverse.group.domain.GroupMemberRole;
import cluverse.group.domain.GroupStatus;
import cluverse.group.domain.GroupVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardReaderTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardQueryRepository boardQueryRepository;

    @InjectMocks
    private BoardReader boardReader;

    @Test
    void 커뮤니티_보드_홈은_비회원도_읽을_수_있다() {
        // given
        Board board = createBoard(1L, BoardType.DEPARTMENT, "컴퓨터공학", null, 0);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        // when
        var result = boardReader.readBoardHome(null, false, 1L);

        // then
        assertThat(result.isReadable()).isTrue();
        assertThat(result.isWritable()).isFalse();
        assertThat(result.defaultTab()).isEqualTo(cluverse.board.service.response.BoardHomeTabType.FEED);
    }

    @Test
    void 그룹_보드_상세는_멤버_권한을_반영한다() {
        // given
        Board board = createBoard(31L, BoardType.GROUP, "AI 프로젝트 게시판", null, 0);
        when(boardRepository.findById(31L)).thenReturn(Optional.of(board));
        when(boardQueryRepository.findGroupBoardMap(Set.of(31L), 7L)).thenReturn(Map.of(
                31L, new BoardGroupQueryDto(
                        31L,
                        100L,
                        "AI 프로젝트",
                        GroupVisibility.PUBLIC,
                        GroupStatus.ACTIVE,
                        GroupMemberRole.ADMIN
                )
        ));
        when(boardQueryRepository.findChildBoardSummaries(31L, true)).thenReturn(List.of());

        // when
        var result = boardReader.readBoardDetail(7L, true, 31L);

        // then
        assertThat(result.isReadable()).isTrue();
        assertThat(result.isWritable()).isTrue();
        assertThat(result.isManageable()).isTrue();
        assertThat(result.group()).isNotNull();
        assertThat(result.group().groupId()).isEqualTo(100L);
    }

    @Test
    void 그룹_보드_상세는_비멤버면_조회할_수_없다() {
        // given
        Board board = createBoard(31L, BoardType.GROUP, "AI 프로젝트 게시판", null, 0);
        when(boardRepository.findById(31L)).thenReturn(Optional.of(board));
        when(boardQueryRepository.findGroupBoardMap(Set.of(31L), 7L)).thenReturn(Map.of());

        // when, then
        assertThatThrownBy(() -> boardReader.readBoardDetail(7L, true, 31L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("게시판 조회 권한이 없습니다.");
    }

    @Test
    void 종료된_그룹의_보드는_기존_멤버도_조회할_수_없다() {
        // given
        Board board = createBoard(31L, BoardType.GROUP, "AI 프로젝트 게시판", null, 0);
        when(boardRepository.findById(31L)).thenReturn(Optional.of(board));
        when(boardQueryRepository.findGroupBoardMap(Set.of(31L), 7L)).thenReturn(Map.of(
                31L, new BoardGroupQueryDto(
                        31L,
                        100L,
                        "AI 프로젝트",
                        GroupVisibility.PUBLIC,
                        GroupStatus.CLOSED,
                        GroupMemberRole.ADMIN
                )
        ));

        // when, then
        assertThatThrownBy(() -> boardReader.readBoardDetail(7L, true, 31L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("게시판 조회 권한이 없습니다.");
    }

    @Test
    void 보드_디렉토리는_부모_기준으로_하위_게시판만_반환한다() {
        // given
        Board parentBoard = createBoard(10L, BoardType.DEPARTMENT, "컴퓨터공학", null, 0);
        when(boardRepository.findById(10L)).thenReturn(Optional.of(parentBoard));
        when(boardQueryRepository.findBoardSummaries(BoardType.DEPARTMENT, true)).thenReturn(List.of(
                new BoardQueryDto(10L, BoardType.DEPARTMENT, "컴퓨터공학", "설명", null, 0, 1, true, 2L),
                new BoardQueryDto(11L, BoardType.DEPARTMENT, "소프트웨어공학", "설명", 10L, 1, 1, true, 0L),
                new BoardQueryDto(12L, BoardType.DEPARTMENT, "인공지능", "설명", 10L, 1, 2, true, 0L),
                new BoardQueryDto(20L, BoardType.DEPARTMENT, "경영학", "설명", null, 0, 3, true, 0L)
        ));

        // when
        var result = boardReader.readBoardDirectory(
                null,
                false,
                new BoardSearchRequest(null, null, 10L, 1, true)
        );

        // then
        assertThat(result.boards()).hasSize(2);
        assertThat(result.boards()).extracting("boardId").containsExactly(11L, 12L);
    }

    private Board createBoard(Long boardId, BoardType boardType, String name, Long parentId, int depth) {
        Board board = Board.create(boardType, name, "설명", parentId, depth, 1, true);
        ReflectionTestUtils.setField(board, "id", boardId);
        return board;
    }
}
