package cluverse.post.repository;

import cluverse.post.domain.Post;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepositoryCustom {

    Optional<Post> findActiveByIdForUpdate(Long postId);

    int increaseViewCount(Long postId);

    int increaseLikeCount(Long postId);

    int decreaseLikeCount(Long postId);

    int increaseBookmarkCount(Long postId);

    int decreaseBookmarkCount(Long postId);

    Optional<Post> findWithImagesById(Long postId);

    List<Post> findAllWithImagesByIdIn(Collection<Long> postIds);
}
