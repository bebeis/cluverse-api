package cluverse.comment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentPlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_place_id")
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;
    @Column(nullable = false)
    private Long placeId;
    private Long authorUniversityId;
    private Long universityCampusId;
    @Column(nullable = false)
    private boolean recommended;

    private CommentPlace(Comment comment, Long placeId, Long authorUniversityId,
                         Long universityCampusId, boolean recommended) {
        this.comment = comment;
        this.placeId = placeId;
        this.authorUniversityId = authorUniversityId;
        this.universityCampusId = universityCampusId;
        this.recommended = recommended;
    }

    public static CommentPlace of(Comment comment, Long placeId, Long authorUniversityId,
                                  Long universityCampusId, boolean recommended) {
        return new CommentPlace(comment, placeId, authorUniversityId, universityCampusId, recommended);
    }
}
