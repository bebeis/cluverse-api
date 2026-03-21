package cluverse.post.service;

public interface PostAccessService {

    void validatePostExists(Long postId);

    void validateReadablePost(Long memberId, Long postId);

    void validateWritablePost(Long memberId, Long postId);
}
