package cluverse.post.domain;

import jakarta.persistence.*;
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

    @Column(nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TINYINT", nullable = false)
    private int displayOrder;

    private PostImage(Post post, String imageUrl, int displayOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public static PostImage of(Post post, String imageUrl, int displayOrder) {
        return new PostImage(post, imageUrl, displayOrder);
    }
}
