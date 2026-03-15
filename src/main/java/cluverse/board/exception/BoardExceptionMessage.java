package cluverse.board.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardExceptionMessage {

    BOARD_NOT_FOUND("존재하지 않는 게시판입니다."),
    BOARD_ACCESS_DENIED("게시판 관리 권한이 없습니다."),
    BOARD_GROUP_TYPE_NOT_SUPPORTED("그룹 게시판은 그룹 도메인에서 관리해야 합니다."),
    BOARD_PARENT_NOT_FOUND("상위 게시판을 찾을 수 없습니다."),
    BOARD_PARENT_TYPE_MISMATCH("상위 게시판과 동일한 타입의 게시판만 생성할 수 있습니다."),
    BOARD_DEPTH_EXCEEDED("게시판 깊이는 최대 3단계까지 허용됩니다."),
    BOARD_HAS_ACTIVE_CHILDREN("활성 하위 게시판이 존재하여 삭제할 수 없습니다.");

    private final String message;
}
