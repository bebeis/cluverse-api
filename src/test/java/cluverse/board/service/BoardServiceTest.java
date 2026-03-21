package cluverse.board.service;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.service.implement.BoardReader;
import cluverse.board.service.implement.BoardWriter;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.common.exception.ForbiddenException;
import cluverse.member.service.implement.MemberReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardReader boardReader;

    @Mock
    private BoardWriter boardWriter;

    @Mock
    private MemberReader memberReader;

    @InjectMocks
    private BoardService boardService;

    @Test
    void 관리자는_게시판을_생성할_수_있다() {
        // given
        BoardCreateRequest request = new BoardCreateRequest(
                BoardType.DEPARTMENT,
                "컴퓨터공학",
                "전국 컴퓨터공학 메인 게시판",
                null,
                1,
                true
        );
        Board board = createBoard(101L, BoardType.DEPARTMENT, "컴퓨터공학");
        when(memberReader.isAdmin(1L)).thenReturn(true);
        when(boardWriter.create(request)).thenReturn(board);

        // when
        var result = boardService.createBoard(1L, request);

        // then
        assertThat(result.boardId()).isEqualTo(101L);
        assertThat(result.boardType()).isEqualTo(BoardType.DEPARTMENT);
        verify(memberReader).isAdmin(1L);
        verify(boardWriter).create(request);
    }

    @Test
    void 관리자는_게시판을_수정할_수_있다() {
        // given
        BoardUpdateRequest request = new BoardUpdateRequest(
                "컴퓨터공학",
                "설명 수정",
                3,
                true
        );
        Board board = createBoard(101L, BoardType.DEPARTMENT, "컴퓨터공학");
        when(memberReader.isAdmin(1L)).thenReturn(true);
        when(boardReader.readOrThrow(101L)).thenReturn(board);

        // when
        var result = boardService.updateBoard(1L, 101L, request);

        // then
        assertThat(result.boardId()).isEqualTo(101L);
        verify(memberReader).isAdmin(1L);
        verify(boardReader).readOrThrow(101L);
        verify(boardWriter).update(board, request);
    }

    @Test
    void 관리자는_게시판을_삭제할_수_있다() {
        // given
        Board board = createBoard(101L, BoardType.DEPARTMENT, "컴퓨터공학");
        when(memberReader.isAdmin(1L)).thenReturn(true);
        when(boardReader.readOrThrow(101L)).thenReturn(board);

        // when
        boardService.deleteBoard(1L, 101L);

        // then
        verify(memberReader).isAdmin(1L);
        verify(boardReader).readOrThrow(101L);
        verify(boardWriter).delete(board);
    }

    @Test
    void 관리자가_아니면_게시판을_생성할_수_없다() {
        // given
        BoardCreateRequest request = new BoardCreateRequest(
                BoardType.DEPARTMENT,
                "컴퓨터공학",
                "전국 컴퓨터공학 메인 게시판",
                null,
                1,
                true
        );
        when(memberReader.isAdmin(1L)).thenReturn(false);

        // when, then
        assertThatThrownBy(() -> boardService.createBoard(1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("게시판 관리 권한이 없습니다.");
    }

    @Test
    void 그룹_서비스는_그룹_게시판을_생성할_수_있다() {
        // given
        Board board = createBoard(201L, BoardType.GROUP, "AI 프로젝트");
        when(boardWriter.createGroupBoard("AI 프로젝트", "그룹 소개")).thenReturn(board);

        // when
        Board result = boardService.createGroupBoard("AI 프로젝트", "그룹 소개");

        // then
        assertThat(result.getId()).isEqualTo(201L);
        verify(boardWriter).createGroupBoard("AI 프로젝트", "그룹 소개");
    }

    @Test
    void 그룹_서비스는_그룹_게시판_메타데이터를_수정할_수_있다() {
        // given
        Board board = createBoard(201L, BoardType.GROUP, "AI 프로젝트");
        when(boardReader.readOrThrow(201L)).thenReturn(board);

        // when
        boardService.updateGroupBoard(201L, "AI 프로젝트 시즌2", "새 소개");

        // then
        verify(boardReader).readOrThrow(201L);
        verify(boardWriter).updateGroupBoard(board, "AI 프로젝트 시즌2", "새 소개");
    }

    @Test
    void 그룹_서비스는_그룹_게시판을_비활성화할_수_있다() {
        // given
        Board board = createBoard(201L, BoardType.GROUP, "AI 프로젝트");
        when(boardReader.readOrThrow(201L)).thenReturn(board);

        // when
        boardService.deactivateGroupBoard(201L);

        // then
        verify(boardReader).readOrThrow(201L);
        verify(boardWriter).deactivateGroupBoard(board);
    }

    private Board createBoard(Long boardId, BoardType boardType, String name) {
        Board board = Board.create(boardType, name, "설명", null, 0, 1, true);
        ReflectionTestUtils.setField(board, "id", boardId);
        return board;
    }
}
