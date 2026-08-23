package cluverse.home.repository;

import cluverse.board.domain.Board;
import cluverse.board.domain.BoardType;
import cluverse.board.repository.BoardRepository;
import cluverse.comment.domain.Comment;
import cluverse.comment.domain.PostCommentActivity;
import cluverse.comment.repository.CommentRepository;
import cluverse.comment.repository.PostCommentActivityRepository;
import cluverse.common.config.QuerydslConfig;
import cluverse.group.domain.Group;
import cluverse.group.domain.GroupActivityType;
import cluverse.group.domain.GroupCategory;
import cluverse.group.domain.GroupVisibility;
import cluverse.group.repository.GroupRepository;
import cluverse.home.repository.dto.HomeBoardQueryResult;
import cluverse.home.repository.dto.RecentCommentedPostQueryResult;
import cluverse.interest.domain.Interest;
import cluverse.interest.repository.InterestRepository;
import cluverse.major.domain.Major;
import cluverse.major.repository.MajorRepository;
import cluverse.member.domain.Block;
import cluverse.member.domain.MajorType;
import cluverse.member.domain.Member;
import cluverse.member.repository.BlockRepository;
import cluverse.member.repository.MemberRepository;
import cluverse.post.domain.Post;
import cluverse.post.domain.PostCategory;
import cluverse.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({HomeQueryRepository.class, QuerydslConfig.class})
class HomeQueryRepositoryTest {

    @Autowired
    private HomeQueryRepository homeQueryRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostCommentActivityRepository activityRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void addVisibleCreatedAtForH2() {
        jdbcTemplate.execute(
                "ALTER TABLE comment ADD COLUMN IF NOT EXISTS visible_created_at TIMESTAMP"
        );
    }

    @Test
    void 관심_학과와_관심사와_가입_그룹의_게시판을_커서로_조회한다() {
        // given
        Board majorBoard = boardRepository.save(board(BoardType.DEPARTMENT, "컴퓨터공학"));
        Board interestBoard = boardRepository.save(board(BoardType.INTEREST, "AI"));
        Board groupBoard = boardRepository.save(Board.createGroupBoard("프로젝트", "그룹 게시판"));
        boardRepository.save(board(BoardType.INTEREST, "관심 없음"));

        Major major = majorRepository.save(major(majorBoard.getId(), "컴퓨터공학"));
        Interest interest = interestRepository.save(interest(interestBoard.getId(), "AI"));
        Member viewer = memberRepository.save(Member.create("viewer", 1L));
        viewer.addMajor(major.getId(), MajorType.PRIMARY);
        viewer.addInterest(interest.getId());
        memberRepository.saveAndFlush(viewer);
        groupRepository.save(Group.create(
                groupBoard.getId(), "프로젝트", "설명", null,
                GroupCategory.PROJECT, GroupActivityType.ONLINE, null,
                GroupVisibility.PRIVATE, viewer.getId(), 10, List.of()
        ));

        // when
        List<HomeBoardQueryResult> firstPage = homeQueryRepository.findFavoriteBoards(
                viewer.getId(), null, 2
        );
        List<HomeBoardQueryResult> secondPage = homeQueryRepository.findFavoriteBoards(
                viewer.getId(), firstPage.getLast().boardId(), 2
        );

        // then
        assertThat(firstPage).extracting(HomeBoardQueryResult::boardId)
                .containsExactly(majorBoard.getId(), interestBoard.getId());
        assertThat(secondPage).extracting(HomeBoardQueryResult::boardId)
                .containsExactly(groupBoard.getId());
    }

    @Test
    void 활동_투영에서_접근_가능한_최근_댓글_글만_조회한다() {
        // given
        Board publicBoard = boardRepository.save(board(BoardType.INTEREST, "백엔드"));
        Board joinedGroupBoard = boardRepository.save(Board.createGroupBoard("참여 그룹", "설명"));
        Board hiddenGroupBoard = boardRepository.save(Board.createGroupBoard("미참여 그룹", "설명"));
        Member viewer = memberRepository.save(Member.create("viewer", 1L));
        Member author = memberRepository.save(Member.create("author", 2L));
        Member blockedAuthor = memberRepository.save(Member.create("blocked", 2L));
        groupRepository.save(Group.create(
                joinedGroupBoard.getId(), "참여 그룹", "설명", null,
                GroupCategory.STUDY, GroupActivityType.HYBRID, null,
                GroupVisibility.PRIVATE, viewer.getId(), 10, List.of()
        ));
        groupRepository.save(Group.create(
                hiddenGroupBoard.getId(), "미참여 그룹", "설명", null,
                GroupCategory.STUDY, GroupActivityType.HYBRID, null,
                GroupVisibility.PRIVATE, author.getId(), 10, List.of()
        ));

        Post olderPost = postRepository.save(post(publicBoard.getId(), author.getId(), "이전 대화"));
        Post latestPost = postRepository.save(post(joinedGroupBoard.getId(), author.getId(), "최근 대화"));
        Post hiddenGroupPost = postRepository.save(post(hiddenGroupBoard.getId(), author.getId(), "숨은 그룹"));
        Post blockedPost = postRepository.save(post(publicBoard.getId(), blockedAuthor.getId(), "차단 글"));

        saveCommentAndActivity(olderPost, author, LocalDateTime.of(2026, 8, 3, 10, 0));
        saveCommentAndActivity(latestPost, author, LocalDateTime.of(2026, 8, 3, 12, 0));
        saveDeletedComment(latestPost, author, LocalDateTime.of(2026, 8, 3, 18, 0));
        saveCommentAndActivity(hiddenGroupPost, author, LocalDateTime.of(2026, 8, 3, 14, 0));
        saveCommentAndActivity(blockedPost, blockedAuthor, LocalDateTime.of(2026, 8, 3, 16, 0));
        blockRepository.save(Block.of(viewer.getId(), blockedAuthor.getId()));

        // when
        List<RecentCommentedPostQueryResult> projected = homeQueryRepository.findRecentCommentedPosts(
                viewer.getId(), 10
        );

        // then
        assertThat(projected).extracting(RecentCommentedPostQueryResult::postId)
                .containsExactly(latestPost.getId(), olderPost.getId());
    }

    private void saveCommentAndActivity(Post post, Member author, LocalDateTime createdAt) {
        Comment comment = Comment.createByMember(
                post.getId(), author.getId(), null, 0, "댓글", false, "127.0.0.1"
        );
        Comment savedComment = commentRepository.saveAndFlush(comment);
        jdbcTemplate.update(
                "UPDATE comment SET created_at = ?, updated_at = ?, visible_created_at = ? WHERE comment_id = ?",
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt),
                savedComment.getId()
        );
        ReflectionTestUtils.setField(savedComment, "createdAt", createdAt);
        ReflectionTestUtils.setField(savedComment, "updatedAt", createdAt);
        activityRepository.saveAndFlush(PostCommentActivity.from(savedComment));
    }

    private void saveDeletedComment(Post post, Member author, LocalDateTime createdAt) {
        Comment comment = Comment.createByMember(
                post.getId(), author.getId(), null, 0, "삭제 댓글", false, "127.0.0.1"
        );
        comment.delete();
        Comment savedComment = commentRepository.saveAndFlush(comment);
        jdbcTemplate.update(
                "UPDATE comment SET created_at = ?, updated_at = ?, visible_created_at = NULL WHERE comment_id = ?",
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt),
                savedComment.getId()
        );
    }

    private Board board(BoardType type, String name) {
        return Board.create(type, name, null, null, 0, 0, true);
    }

    private Major major(Long boardId, String name) {
        Major major = BeanUtils.instantiateClass(Major.class);
        ReflectionTestUtils.setField(major, "boardId", boardId);
        ReflectionTestUtils.setField(major, "name", name);
        ReflectionTestUtils.setField(major, "depth", 0);
        ReflectionTestUtils.setField(major, "displayOrder", 0);
        ReflectionTestUtils.setField(major, "isActive", true);
        return major;
    }

    private Interest interest(Long boardId, String name) {
        Interest interest = BeanUtils.instantiateClass(Interest.class);
        ReflectionTestUtils.setField(interest, "boardId", boardId);
        ReflectionTestUtils.setField(interest, "name", name);
        ReflectionTestUtils.setField(interest, "category", "TECH");
        ReflectionTestUtils.setField(interest, "displayOrder", 0);
        ReflectionTestUtils.setField(interest, "isActive", true);
        return interest;
    }

    private Post post(Long boardId, Long memberId, String title) {
        return Post.createByMember(
                List.of(), List.of(), boardId, memberId, title, "본문",
                PostCategory.INFORMATION, false, false, true, "127.0.0.1"
        );
    }
}
