package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_place_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    @Column(nullable = false)
    private Long placeId;
    @Column(nullable = false)
    private int displayOrder;
    private Long authorUniversityId;
    private Long universityCampusId;
    @Column(nullable = false)
    private boolean recommended;

    private PostPlace(Post post, Long placeId, int displayOrder, Long authorUniversityId,
                      Long universityCampusId, boolean recommended) {
        this.post = post;
        this.placeId = placeId;
        this.displayOrder = displayOrder;
        this.authorUniversityId = authorUniversityId;
        this.universityCampusId = universityCampusId;
        this.recommended = recommended;
    }

    public static PostPlace of(Post post, Long placeId, int displayOrder, Long authorUniversityId,
                               Long universityCampusId, boolean recommended) {
        return new PostPlace(post, placeId, displayOrder, authorUniversityId, universityCampusId, recommended);
    }
}
