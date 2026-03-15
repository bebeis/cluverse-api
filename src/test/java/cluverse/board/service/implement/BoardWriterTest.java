package cluverse.board.service.implement;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.repository.BoardRepository;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardWriterTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardWriter boardWriter;

    @Test
    void 상위_게시판을_기준으로_하위_게시판을_생성한다() {
        // given
        Board parentBoard = Board.create(BoardType.DEPARTMENT, "컴퓨터공학", "설명", null, 0, 1, true);
        ReflectionTestUtils.setField(parentBoard, "id", 10L);
        BoardCreateRequest request = new BoardCreateRequest(
                BoardType.DEPARTMENT,
                "소프트웨어공학",
                "설명",
                10L,
                2,
                true
        );
        when(boardRepository.findById(10L)).thenReturn(Optional.of(parentBoard));
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Board result = boardWriter.create(request);

        // then
        assertThat(result.getParentId()).isEqualTo(10L);
        assertThat(result.getDepth()).isEqualTo(1);
        assertThat(result.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void 그룹_게시판은_운영용_api로_생성할_수_없다() {
        // given
        BoardCreateRequest request = new BoardCreateRequest(
                BoardType.GROUP,
                "그룹 게시판",
                "설명",
                null,
                1,
                true
        );

        // when, then
        assertThatThrownBy(() -> boardWriter.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("그룹 게시판은 그룹 도메인에서 관리해야 합니다.");
    }

    @Test
    void 활성_하위_게시판이_있으면_삭제할_수_없다() {
        // given
        Board board = Board.create(BoardType.DEPARTMENT, "컴퓨터공학", "설명", null, 0, 1, true);
        ReflectionTestUtils.setField(board, "id", 10L);
        when(boardRepository.existsByParentIdAndIsActiveTrue(10L)).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> boardWriter.delete(board))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("활성 하위 게시판이 존재하여 삭제할 수 없습니다.");
    }

    @Test
    void 게시판_수정시_메타데이터를_변경한다() {
        // given
        Board board = Board.create(BoardType.INTEREST, "AI", "설명", null, 0, 1, true);
        BoardUpdateRequest request = new BoardUpdateRequest("AI/ML", "새 설명", 3, false);

        // when
        boardWriter.update(board, request);

        // then
        assertThat(board.getName()).isEqualTo("AI/ML");
        assertThat(board.getDescription()).isEqualTo("새 설명");
        assertThat(board.getDisplayOrder()).isEqualTo(3);
        assertThat(board.isActive()).isFalse();
    }
}
