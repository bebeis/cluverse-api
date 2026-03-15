package cluverse.group.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupRole extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_role_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private String title;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private GroupRole(Group group, String title, int displayOrder) {
        this.group = group;
        this.title = title;
        this.displayOrder = displayOrder;
    }

    public static GroupRole create(Group group, String title, int displayOrder) {
        return new GroupRole(group, title, displayOrder);
    }

    public void update(String title, int displayOrder) {
        this.title = title;
        this.displayOrder = displayOrder;
    }
}
