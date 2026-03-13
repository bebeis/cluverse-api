package cluverse.post.service.response;

public record PostAuthorResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
}
