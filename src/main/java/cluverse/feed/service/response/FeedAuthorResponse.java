package cluverse.feed.service.response;

public record FeedAuthorResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
    private static final String ANONYMOUS_NICKNAME = "익명";
    private static final String BLOCKED_NICKNAME = "차단된 사용자";

    private static FeedAuthorResponse of(Long memberId, String nickname, String profileImageUrl) {
        return new FeedAuthorResponse(memberId, nickname, profileImageUrl);
    }

    public static FeedAuthorResponse blocked() {
        return new FeedAuthorResponse(null, BLOCKED_NICKNAME, null);
    }

    public static FeedAuthorResponse visibleOf(
            boolean isAnonymous,
            boolean isMine,
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
        if (!isAnonymous || isMine) {
            return of(memberId, nickname, profileImageUrl);
        }
        return new FeedAuthorResponse(null, ANONYMOUS_NICKNAME, null);
    }
}
