package cluverse.board.controller;

import cluverse.board.service.BoardService;
import cluverse.board.service.request.BoardSearchRequest;
import cluverse.board.service.response.BoardDetailResponse;
import cluverse.board.service.response.BoardDirectoryResponse;
import cluverse.board.service.response.BoardHomeResponse;
import cluverse.common.api.response.ApiResponse;
import cluverse.common.auth.Login;
import cluverse.common.auth.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ApiResponse<BoardDirectoryResponse> getBoardDirectory(
            @Login LoginMember loginMember,
            @Valid @ModelAttribute BoardSearchRequest request
    ) {
        return ApiResponse.ok(boardService.getBoardDirectory(extractMemberId(loginMember), request));
    }

    @GetMapping("/{boardId}")
    public ApiResponse<BoardDetailResponse> getBoard(
            @Login LoginMember loginMember,
            @PathVariable Long boardId
    ) {
        return ApiResponse.ok(boardService.getBoard(extractMemberId(loginMember), boardId));
    }

    @GetMapping("/{boardId}/home")
    public ApiResponse<BoardHomeResponse> getBoardHome(
            @Login LoginMember loginMember,
            @PathVariable Long boardId
    ) {
        return ApiResponse.ok(boardService.getBoardHome(extractMemberId(loginMember), boardId));
    }

    private Long extractMemberId(LoginMember loginMember) {
        return loginMember == null ? null : loginMember.memberId();
    }
}
