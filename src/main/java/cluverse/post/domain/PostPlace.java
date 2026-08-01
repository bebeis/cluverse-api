package cluverse.post.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @Column(nullable = false)
    private Long postId;
    @Column(nullable = false)
    private Long placeId;
    @Column(nullable = false)
    private int displayOrder;
    private Long authorUniversityId;
    private Long universityCampusId;
    @Column(nullable = false)
    private boolean recommended;

    private PostPlace(Long postId, Long placeId, int displayOrder, Long authorUniversityId,
                      Long universityCampusId, boolean recommended) {
        this.postId = postId;
        this.placeId = placeId;
        this.displayOrder = displayOrder;
        this.authorUniversityId = authorUniversityId;
        this.universityCampusId = universityCampusId;
        this.recommended = recommended;
    }

    public static PostPlace of(Long postId, Long placeId, int displayOrder, Long authorUniversityId,
                               Long universityCampusId, boolean recommended) {
        return new PostPlace(postId, placeId, displayOrder, authorUniversityId, universityCampusId, recommended);
    }
}
