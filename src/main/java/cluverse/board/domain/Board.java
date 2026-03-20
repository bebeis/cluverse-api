package cluverse.board.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false)
    private BoardType boardType;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean isActive;

    @Builder(access = AccessLevel.PRIVATE)
    private Board(BoardType boardType,
                  String name,
                  String description,
                  Long parentId,
                  int depth,
                  int displayOrder,
                  boolean isActive) {
        this.boardType = boardType;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.depth = depth;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    public static Board create(BoardType boardType,
                               String name,
                               String description,
                               Long parentId,
                               int depth,
                               int displayOrder,
                               boolean isActive) {
        return Board.builder()
                .boardType(boardType)
                .name(name)
                .description(description)
                .parentId(parentId)
                .depth(depth)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .build();
    }

    public static Board createGroupBoard(String name, String description) {
        return create(BoardType.GROUP, name, description, null, 0, 0, true);
    }

    public void updateGroupMetadata(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description, int displayOrder, boolean isActive) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
