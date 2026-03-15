package cluverse.recruitment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_id")
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = RecruitmentPositionListConverter.class)
    @Column(columnDefinition = "json")
    private List<RecruitmentPosition> positions = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String requirements;

    private String duration;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Column(name = "process_description", columnDefinition = "TEXT")
    private String processDescription;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentStatus status;

    @Column(name = "application_count", nullable = false)
    private int applicationCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<FormItem> formItems = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Recruitment(Long groupId,
                        Long authorId,
                        String title,
                        String description,
                        List<RecruitmentPosition> positions,
                        String requirements,
                        String duration,
                        String goal,
                        String processDescription,
                        LocalDateTime deadline) {
        this.groupId = groupId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.positions = positions == null ? List.of() : List.copyOf(positions);
        this.requirements = requirements;
        this.duration = duration;
        this.goal = goal;
        this.processDescription = processDescription;
        this.deadline = deadline;
        this.status = RecruitmentStatus.OPEN;
        this.applicationCount = 0;
    }

    public static Recruitment create(Long groupId,
                                     Long authorId,
                                     String title,
                                     String description,
                                     List<RecruitmentPosition> positions,
                                     String requirements,
                                     String duration,
                                     String goal,
                                     String processDescription,
                                     LocalDateTime deadline,
                                     List<FormItem> formItems) {
        Recruitment recruitment = Recruitment.builder()
                .groupId(groupId)
                .authorId(authorId)
                .title(title)
                .description(description)
                .positions(positions)
                .requirements(requirements)
                .duration(duration)
                .goal(goal)
                .processDescription(processDescription)
                .deadline(deadline)
                .build();
        recruitment.replaceFormItems(formItems);
        return recruitment;
    }

    public void update(String title,
                       String description,
                       List<RecruitmentPosition> positions,
                       String requirements,
                       String duration,
                       String goal,
                       String processDescription,
                       LocalDateTime deadline,
                       List<FormItem> formItems) {
        this.title = title;
        this.description = description;
        this.positions = positions == null ? List.of() : List.copyOf(positions);
        this.requirements = requirements;
        this.duration = duration;
        this.goal = goal;
        this.processDescription = processDescription;
        this.deadline = deadline;
        replaceFormItems(formItems);
    }

    public void changeStatus(RecruitmentStatus status) {
        this.status = status;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isOpen() {
        return status == RecruitmentStatus.OPEN && (deadline == null || deadline.isAfter(LocalDateTime.now()));
    }

    public void increaseApplicationCount() {
        this.applicationCount++;
    }

    public void decreaseApplicationCount() {
        this.applicationCount--;
    }

    private void replaceFormItems(List<FormItem> formItems) {
        this.formItems.clear();
        if (formItems == null) {
            return;
        }
        formItems.forEach(formItem -> {
            formItem.assignRecruitment(this);
            this.formItems.add(formItem);
        });
    }
}
