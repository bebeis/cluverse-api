package cluverse.post.repository;

import cluverse.post.domain.Post;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepositoryCustom {

    Optional<Post> findWithImagesById(Long postId);

    List<Post> findAllWithImagesByIdIn(Collection<Long> postIds);
}
