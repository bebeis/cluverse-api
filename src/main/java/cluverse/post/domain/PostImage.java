package cluverse.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column
    private String imageUrl;

    private String contentKey;

    private String thumbnailKey;

    @Column(columnDefinition = "TINYINT", nullable = false)
    private int displayOrder;

    private PostImage(Post post, String imageUrl, String contentKey, String thumbnailKey, int displayOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.contentKey = contentKey;
        this.thumbnailKey = thumbnailKey;
        this.displayOrder = displayOrder;
    }

    public static PostImage of(Post post, String imageUrl, int displayOrder) {
        return new PostImage(post, imageUrl, null, null, displayOrder);
    }

    public static PostImage processed(
            Post post,
            String contentKey,
            String thumbnailKey,
            int displayOrder
    ) {
        return new PostImage(post, null, contentKey, thumbnailKey, displayOrder);
    }
}
