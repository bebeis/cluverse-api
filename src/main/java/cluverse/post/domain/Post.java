package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "post_tag", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag_name")
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private Long memberId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status = PostStatus.ACTIVE;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Builder.Default
    private boolean isAnonymous = false;
    @Builder.Default
    private boolean isPinned = false;
    @Builder.Default
    private boolean isExternalVisible = true;

    private int viewCount;
    private int likeCount;
    private int commentCount;
    private int bookmarkCount;
    private LocalDateTime deletedAt;
    private String clientIp;

    public static Post createByMember(List<String> tags, Long boardId, Long memberId, String title, String content, boolean isAnonymous, String clientIp) {
        Post post = Post.builder()
                .boardId(boardId)
                .memberId(memberId)
                .title(title)
                .content(content)
                .isAnonymous(isAnonymous)
                .clientIp(clientIp)
                .build();

        if (tags != null) {
            post.tags.addAll(tags);
        }

        return post;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }
}
