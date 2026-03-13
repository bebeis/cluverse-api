package cluverse.post.service.response;

public record PostAuthorResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
    private static final String ANONYMOUS_NICKNAME = "익명";

    private static PostAuthorResponse of(Long memberId, String nickname, String profileImageUrl) {
        return new PostAuthorResponse(memberId, nickname, profileImageUrl);
    }

    public static PostAuthorResponse visibleOf(
            boolean isAnonymous,
            boolean isMine,
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
        if (!isAnonymous || isMine) {
            return of(memberId, nickname, profileImageUrl);
        }
        return new PostAuthorResponse(null, ANONYMOUS_NICKNAME, null);
    }
}
