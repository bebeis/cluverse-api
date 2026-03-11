package cluverse.interest.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean isActive = true;
}
