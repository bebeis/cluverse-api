package cluverse.comment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status = CommentStatus.ACTIVE;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int replyCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "client_ip")
    private String clientIp;

    private Comment(Long postId, Long memberId, Long parentId, int depth, String content,
                    boolean isAnonymous, String clientIp) {
        this.postId = postId;
        this.memberId = memberId;
        this.parentId = parentId;
        this.depth = depth;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.clientIp = clientIp;
        this.status = CommentStatus.ACTIVE;
        this.likeCount = 0;
        this.replyCount = 0;
    }

    public static Comment createByMember(Long postId, Long memberId, Long parentId, int depth,
                                         String content, boolean isAnonymous, String clientIp) {
        return new Comment(postId, memberId, parentId, depth, content, isAnonymous, clientIp);
    }

    public void delete() {
        this.status = CommentStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isAuthor(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isActive() {
        return status == CommentStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == CommentStatus.DELETED;
    }
}
