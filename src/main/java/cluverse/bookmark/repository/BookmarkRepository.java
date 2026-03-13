package cluverse.bookmark.repository;

import cluverse.bookmark.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    @Query("""
            select bookmark.postId
            from Bookmark bookmark
            where bookmark.memberId = :memberId
              and bookmark.postId in :postIds
            """)
    List<Long> findPostIdsByMemberIdAndPostIdIn(
            @Param("memberId") Long memberId,
            @Param("postIds") Collection<Long> postIds
    );
}
