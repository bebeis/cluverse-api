package cluverse.comment.service.response;

public record CommentAuthorResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
    private static final String ANONYMOUS_NICKNAME = "익명";

    private static CommentAuthorResponse of(Long memberId, String nickname, String profileImageUrl) {
        return new CommentAuthorResponse(memberId, nickname, profileImageUrl);
    }

    public static CommentAuthorResponse visibleOf(
            boolean isAnonymous,
            boolean isMine,
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
        if (!isAnonymous || isMine) {
            return of(memberId, nickname, profileImageUrl);
        }
        return new CommentAuthorResponse(null, ANONYMOUS_NICKNAME, null);
    }
}
