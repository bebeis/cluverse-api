package cluverse.feed.repository;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.repository.BoardRepository;
import cluverse.common.config.QuerydslConfig;
import cluverse.feed.repository.dto.FeedPageQueryResult;
import cluverse.feed.service.request.FollowingFeedScope;
import cluverse.feed.service.request.HomeFeedFilter;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.repository.GroupRepository;
import cluverse.interest.domain.Interest;
import cluverse.interest.repository.InterestRepository;
import cluverse.member.domain.Block;
import cluverse.member.domain.Follow;
import cluverse.member.domain.Member;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.FollowRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.meta.domain.PostBookmarkCount;
import cluverse.meta.domain.PostCommentCount;
import cluverse.meta.domain.PostLikeCount;
import cluverse.meta.repository.PostBookmarkCountRepository;
import cluverse.meta.repository.PostCommentCountRepository;
import cluverse.meta.repository.PostLikeCountRepository;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostRepository;
import cluverse.reaction.domain.PostBookmark;
import cluverse.reaction.domain.PostLike;
import cluverse.reaction.repository.PostBookmarkRepository;
import cluverse.reaction.repository.PostLikeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({FeedQueryRepository.class, QuerydslConfig.class})
class FeedQueryRepositoryTest {

    @Autowired
    private FeedQueryRepository feedQueryRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private PostLikeCountRepository postLikeCountRepository;

    @Autowired
    private PostCommentCountRepository postCommentCountRepository;

    @Autowired
    private PostBookmarkCountRepository postBookmarkCountRepository;

    @Test
    void 홈_피드는_구독_보드와_같은_학교_작성글을_조회하고_차단한_작성자는_제외한다() {
        // given
        Board subscribedBoard = boardRepository.save(createBoard(BoardType.INTEREST, "AI"));
        Board sameUniversityBoard = boardRepository.save(createBoard(BoardType.DEPARTMENT, "컴퓨터공학"));
        Board unrelatedBoard = boardRepository.save(createBoard(BoardType.INTEREST, "주식"));

        Interest aiInterest = interestRepository.save(createInterest(subscribedBoard.getId(), "AI"));

        Member viewer = memberRepository.save(Member.create("viewer", 1L));
        viewer.addInterest(aiInterest.getId());
        memberRepository.save(viewer);

        Member subscribedAuthor = memberRepository.save(Member.create("subscribed-author", 2L));
        Member sameUniversityAuthor = memberRepository.save(Member.create("same-university-author", 1L));
        Member blockedAuthor = memberRepository.save(Member.create("blocked-author", 1L));
        Member unrelatedAuthor = memberRepository.save(Member.create("unrelated-author", 3L));

        Post subscribedPost = postRepository.save(createPost(
                subscribedBoard.getId(),
                subscribedAuthor.getId(),
                "구독 보드 글",
                LocalDateTime.of(2026, 3, 16, 10, 0)
        ));
        Post sameUniversityPost = postRepository.save(createPost(
                sameUniversityBoard.getId(),
                sameUniversityAuthor.getId(),
                "같은 학교 글",
                LocalDateTime.of(2026, 3, 16, 11, 0)
        ));
        postRepository.save(createPost(
                sameUniversityBoard.getId(),
                blockedAuthor.getId(),
                "차단된 작성자 글",
                LocalDateTime.of(2026, 3, 16, 12, 0)
        ));
        postRepository.save(createPost(
                unrelatedBoard.getId(),
                unrelatedAuthor.getId(),
                "무관한 글",
                LocalDateTime.of(2026, 3, 16, 9, 0)
        ));

        blockRepository.save(Block.of(viewer.getId(), blockedAuthor.getId()));
        postLikeRepository.save(PostLike.of(sameUniversityPost.getId(), viewer.getId()));
        postBookmarkRepository.save(PostBookmark.of(viewer.getId(), sameUniversityPost.getId()));

        // when
        FeedPageQueryResult result = feedQueryRepository.findHomeFeed(
                viewer.getId(),
                HomeFeedFilter.ALL,
                feedQueryRepository.findUniversityId(viewer.getId()),
                feedQueryRepository.findSubscribedBoardIds(viewer.getId()),
                feedQueryRepository.findBlockedMemberIds(viewer.getId()),
                feedQueryRepository.findMyGroupBoardIds(viewer.getId()),
                null,
                null,
                10
        );

        // then
        assertThat(result.posts()).extracting("postId")
                .containsExactly(sameUniversityPost.getId(), subscribedPost.getId());
        assertThat(result.posts().getFirst().liked()).isTrue();
        assertThat(result.posts().getFirst().bookmarked()).isTrue();
    }

    @Test
    void 팔로잉_피드는_팔로우한_작성자와_내_그룹_게시글만_모아서_조회한다() {
        // given
        Board publicBoard = boardRepository.save(createBoard(BoardType.INTEREST, "백엔드"));
        Board groupBoard = boardRepository.save(Board.createGroupBoard("AI 프로젝트", "그룹 보드"));

        Member viewer = memberRepository.save(Member.create("group-viewer", 1L));
        Member followingAuthor = memberRepository.save(Member.create("following-author", 2L));
        Member groupAuthor = memberRepository.save(Member.create("group-author", 2L));
        Member stranger = memberRepository.save(Member.create("stranger", 3L));

        followRepository.save(Follow.of(viewer.getId(), followingAuthor.getId()));
        groupRepository.save(Group.create(
                groupBoard.getId(),
                "AI 프로젝트",
                "설명",
                null,
                GroupCategory.PROJECT,
                GroupActivityType.HYBRID,
                "서울",
                GroupVisibility.PUBLIC,
                viewer.getId(),
                10,
                List.of()
        ));

        Post followedPost = postRepository.save(createPost(
                publicBoard.getId(),
                followingAuthor.getId(),
                "팔로우 글",
                LocalDateTime.of(2026, 3, 16, 10, 0)
        ));
        Post myGroupPost = postRepository.save(createPost(
                groupBoard.getId(),
                groupAuthor.getId(),
                "내 그룹 글",
                LocalDateTime.of(2026, 3, 16, 11, 0)
        ));
        postRepository.save(createPost(
                publicBoard.getId(),
                stranger.getId(),
                "무관한 글",
                LocalDateTime.of(2026, 3, 16, 12, 0)
        ));

        // when
        FeedPageQueryResult result = feedQueryRepository.findFollowingFeed(
                viewer.getId(),
                FollowingFeedScope.ALL,
                feedQueryRepository.findFollowingMemberIds(viewer.getId()),
                feedQueryRepository.findMyGroupBoardIds(viewer.getId()),
                feedQueryRepository.findBlockedMemberIds(viewer.getId()),
                null,
                null,
                10
        );

        // then
        assertThat(result.posts()).extracting("postId")
                .containsExactly(myGroupPost.getId(), followedPost.getId());
    }

    @Test
    void 트렌딩_피드는_점수순으로_조회하고_커서를_반환한다() {
        // given
        Board board = boardRepository.save(createBoard(BoardType.INTEREST, "데이터"));
        Member viewer = memberRepository.save(Member.create("trend-viewer", 1L));
        Member author = memberRepository.save(Member.create("trend-author", 2L));

        Post highestScorePost = postRepository.save(createPost(
                board.getId(),
                author.getId(),
                "가장 인기 글",
                LocalDateTime.now().minusHours(2)
        ));
        Post middleScorePost = postRepository.save(createPost(
                board.getId(),
                author.getId(),
                "중간 인기 글",
                LocalDateTime.now().minusHours(3)
        ));
        postRepository.save(createPost(
                board.getId(),
                author.getId(),
                "낮은 인기 글",
                LocalDateTime.now().minusHours(4)
        ));

        saveCounts(highestScorePost.getId(), 10, 5, 2);
        saveCounts(middleScorePost.getId(), 5, 1, 1);

        // when
        FeedPageQueryResult result = feedQueryRepository.findTrendingFeed(
                viewer.getId(),
                LocalDateTime.now().minusDays(7),
                PostCategory.INFORMATION,
                feedQueryRepository.findBlockedMemberIds(viewer.getId()),
                feedQueryRepository.findMyGroupBoardIds(viewer.getId()),
                null,
                null,
                null,
                2
        );

        // then
        assertThat(result.posts()).extracting("postId")
                .containsExactly(highestScorePost.getId(), middleScorePost.getId());
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotBlank();
    }

    private Board createBoard(BoardType boardType, String name) {
        return Board.create(boardType, name, null, null, 0, 0, true);
    }

    private Interest createInterest(Long boardId, String name) {
        Interest interest = BeanUtils.instantiateClass(Interest.class);
        ReflectionTestUtils.setField(interest, "boardId", boardId);
        ReflectionTestUtils.setField(interest, "name", name);
        ReflectionTestUtils.setField(interest, "category", "TECH");
        ReflectionTestUtils.setField(interest, "displayOrder", 0);
        ReflectionTestUtils.setField(interest, "isActive", true);
        setAuditFields(interest, LocalDateTime.of(2026, 3, 16, 0, 0));
        return interest;
    }

    private Post createPost(Long boardId, Long memberId, String title, LocalDateTime createdAt) {
        Post post = Post.createByMember(
                List.of("tag"),
                List.of("https://cdn.example.com/posts/thumb.png"),
                boardId,
                memberId,
                title,
                "본문입니다.",
                PostCategory.INFORMATION,
                false,
                false,
                true,
                "127.0.0.1"
        );
        ReflectionTestUtils.setField(post, "viewCount", 10);
        setAuditFields(post, createdAt);
        return post;
    }

    private void saveCounts(Long postId, int likeCount, int commentCount, int bookmarkCount) {
        postLikeCountRepository.save(PostLikeCount.of(postId, likeCount));
        postCommentCountRepository.save(PostCommentCount.of(postId, commentCount));
        postBookmarkCountRepository.save(PostBookmarkCount.of(postId, bookmarkCount));
    }

    private void setAuditFields(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
        ReflectionTestUtils.setField(target, "updatedAt", createdAt);
    }
}
