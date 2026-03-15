package cluverse.reaction.repository;

import cluverse.reaction.domain.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    Optional<PostBookmark> findByMemberIdAndPostId(Long memberId, Long postId);

    @Query("""
            select postBookmark.postId
            from PostBookmark postBookmark
            where postBookmark.memberId = :memberId
              and postBookmark.postId in :postIds
            """)
    List<Long> findPostIdsByMemberIdAndPostIdIn(
            @Param("memberId") Long memberId,
            @Param("postIds") Collection<Long> postIds
    );
}
