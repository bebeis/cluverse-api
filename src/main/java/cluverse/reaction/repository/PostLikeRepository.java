package cluverse.reaction.repository;

import cluverse.reaction.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    @Query("""
            select postLike.postId
            from PostLike postLike
            where postLike.memberId = :memberId
              and postLike.postId in :postIds
            """)
    List<Long> findPostIdsByMemberIdAndPostIdIn(
            @Param("memberId") Long memberId,
            @Param("postIds") Collection<Long> postIds
    );
}
