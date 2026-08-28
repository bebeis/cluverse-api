package cluverse.post.service;

import cluverse.board.service.implement.BoardReader;
import cluverse.comment.service.implement.CommentReader;
import cluverse.comment.service.response.CommentLastRepliedPost;
import cluverse.member.service.implement.MemberReader;
import cluverse.post.domain.Post;
import cluverse.post.service.implement.PostAccessReader;
import cluverse.post.service.implement.PostReader;
import cluverse.post.service.response.PostDetailResponse;
import cluverse.post.service.response.PostTitleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostAccessReader postAccessReader;
    private final PostReader postReader;
    private final BoardReader boardReader;
    private final MemberReader memberReader;
    private final CommentReader commentReader;

    public PostDetailResponse readPost(Long memberId, Long postId) {
        Post post = postAccessReader.readOrThrow(postId);
        boardReader.validateReadable(memberId, post.getBoardId());
        return PostDetailResponse.from(postReader.readPostDetail(memberId, postId));
    }

    public void validatePostExists(Long postId) {
        postAccessReader.validatePostExists(postId);
    }

    public void validateReadablePost(Long memberId, Long postId) {
        postAccessReader.validateReadablePost(memberId, postId);
    }

    public void validateWritablePost(Long memberId, Long postId) {
        postAccessReader.validateWritablePost(memberId, postId);
    }

    public List<PostTitleResponse> getRecentCommentRepliedPosts(Long size) {
        List<CommentLastRepliedPost> commentLastRepliedPosts = commentReader.readRecentCommentRepliedPosts(size);
        if (commentLastRepliedPosts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = commentLastRepliedPosts.stream()
                .map(CommentLastRepliedPost::postId)
                .toList();
        Map<Long, Post> postMap = postAccessReader.readPosts(postIds).stream()
                .filter(Post::isActive)
                .collect(toMap(Post::getId, Function.identity()));

        return commentLastRepliedPosts.stream()
                .map(commentLastRepliedPost -> {
                    Post post = postMap.get(commentLastRepliedPost.postId());
                    if (post == null) {
                        return null;
                    }
                    return new PostTitleResponse(
                            post.getId(),
                            post.getTitle(),
                            commentLastRepliedPost.lastCommentRepliedAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
