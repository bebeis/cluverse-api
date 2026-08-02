package cluverse.comment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    private static final DateTimeFormatter PATH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    static final int MAX_PATH_LENGTH = 255;

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

    @Column(name = "client_request_id")
    private String clientRequestId;

    @Column(length = 255)
    private String path;

    @OneToOne(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private CommentPlace place;

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

    public static Comment createByMember(Long postId, Long memberId, Long parentId, int depth,
                                         String content, boolean isAnonymous, String clientIp,
                                         String clientRequestId) {
        Comment comment = createByMember(postId, memberId, parentId, depth, content, isAnonymous, clientIp);
        comment.clientRequestId = clientRequestId;
        return comment;
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

    public void attachPlace(Long placeId, Long authorUniversityId, Long universityCampusId, boolean recommended) {
        this.place = CommentPlace.of(this, placeId, authorUniversityId, universityCampusId, recommended);
    }

    public void assignPath(Comment parentComment) {
        if (id == null || getCreatedAt() == null) {
            throw new IllegalStateException("댓글 저장 이후에 path를 생성할 수 있습니다.");
        }
        String pathSegment = PATH_TIME_FORMATTER.format(getCreatedAt())
                + "-"
                + String.format(Locale.ROOT, "%020d", id);
        if (parentComment == null) {
            assignValidatedPath(pathSegment);
            return;
        }
        if (parentComment.path == null || parentComment.path.isBlank()) {
            throw new IllegalStateException("부모 댓글의 path가 필요합니다.");
        }
        assignValidatedPath(parentComment.path + "/" + pathSegment);
    }

    private void assignValidatedPath(String generatedPath) {
        if (generatedPath.length() > MAX_PATH_LENGTH) {
            throw new IllegalStateException("댓글 path 길이는 255자를 초과할 수 없습니다.");
        }
        this.path = generatedPath;
    }
}
