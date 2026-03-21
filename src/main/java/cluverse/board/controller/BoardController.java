package cluverse.board.controller;

import cluverse.board.service.BoardQueryService;
import cluverse.board.service.BoardService;
import cluverse.board.service.request.BoardCreateRequest;
import cluverse.board.service.request.BoardSearchRequest;
import cluverse.board.service.request.BoardUpdateRequest;
import cluverse.board.service.response.BoardAdminResponse;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardQueryService boardQueryService;
    private final BoardService boardService;

    @GetMapping
    public ApiResponse<BoardDirectoryResponse> getBoardDirectory(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute BoardSearchRequest request
    ) {
        return ApiResponse.ok(boardQueryService.getBoardDirectory(extractMemberId(loginMember), request));
    }

    @GetMapping("/{boardId}")
    public ApiResponse<BoardDetailResponse> getBoard(
            @Login LoginMember loginMember,
            @PathVariable Long boardId
    ) {
        return ApiResponse.ok(boardQueryService.getBoard(extractMemberId(loginMember), boardId));
    }

    @GetMapping("/{boardId}/home")
    public ApiResponse<BoardHomeResponse> getBoardHome(
            @Login LoginMember loginMember,
            @PathVariable Long boardId
    ) {
        return ApiResponse.ok(boardQueryService.getBoardHome(extractMemberId(loginMember), boardId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BoardAdminResponse> createBoard(
            @Login LoginMember loginMember,
            @RequestBody @Valid BoardCreateRequest request
    ) {
        return ApiResponse.created(boardService.createBoard(loginMember.memberId(), request));
    }

    @PutMapping("/{boardId}")
    public ApiResponse<BoardAdminResponse> updateBoard(
            @Login LoginMember loginMember,
            @PathVariable Long boardId,
            @RequestBody @Valid BoardUpdateRequest request
    ) {
        return ApiResponse.ok(boardService.updateBoard(loginMember.memberId(), boardId, request));
    }

    @DeleteMapping("/{boardId}")
    public ApiResponse<Void> deleteBoard(
            @Login LoginMember loginMember,
            @PathVariable Long boardId
    ) {
        boardService.deleteBoard(loginMember.memberId(), boardId);
        return ApiResponse.ok();
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
