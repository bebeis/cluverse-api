package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PostImage> images = new ArrayList<>();

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Builder.Default
    private boolean isAnonymous = false;
    @Builder.Default
    private boolean isPinned = false;
    @Builder.Default
    private boolean isExternalVisible = true;

    private int viewCount;
    private LocalDateTime deletedAt;
    private String clientIp;

    public static Post createByMember(List<String> tags, List<String> imageUrls, Long boardId, Long memberId,
                                      String title, String content, PostCategory category, boolean isAnonymous,
                                      boolean isPinned, boolean isExternalVisible, String clientIp) {
        Post post = Post.builder()
                .boardId(boardId)
                .memberId(memberId)
                .title(title)
                .content(content)
                .category(category)
                .isAnonymous(isAnonymous)
                .isPinned(isPinned)
                .isExternalVisible(isExternalVisible)
                .clientIp(clientIp)
                .build();

        post.replaceTags(tags);
        post.replaceImages(imageUrls);
        return post;
    }

    public void update(String title, String content, PostCategory category, List<String> tags, List<String> imageUrls,
                       boolean isAnonymous, boolean isPinned, boolean isExternalVisible) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.isAnonymous = isAnonymous;
        this.isPinned = isPinned;
        this.isExternalVisible = isExternalVisible;
        replaceTags(tags);
        replaceImages(imageUrls);
    }

    public void delete() {
        this.status = PostStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public boolean isAuthor(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isActive() {
        return this.status == PostStatus.ACTIVE;
    }

    public String getThumbnailImageUrl() {
        return images.isEmpty() ? null : images.getFirst().getImageUrl();
    }

    public List<String> getImageUrls() {
        return images.stream()
                .map(PostImage::getImageUrl)
                .toList();
    }

    private void replaceTags(List<String> tags) {
        this.tags.clear();
        if (tags == null) {
            return;
        }
        this.tags.addAll(tags);
    }

    private void replaceImages(List<String> imageUrls) {
        this.images.clear();
        if (imageUrls == null) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            this.images.add(PostImage.of(this, imageUrls.get(i), i));
        }
    }
}
