package cluverse.group.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInterest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_interest_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "interest_id", nullable = false)
    private Long interestId;

    private GroupInterest(Group group, Long interestId) {
        this.group = group;
        this.interestId = interestId;
    }

    public static GroupInterest of(Group group, Long interestId) {
        return new GroupInterest(group, interestId);
    }
}
