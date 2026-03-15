package cluverse.recruitment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormItemAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_item_answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_application_id", nullable = false)
    private RecruitmentApplication application;

    @Column(name = "form_item_id", nullable = false)
    private Long formItemId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    private FormItemAnswer(Long formItemId, String answer) {
        this.formItemId = formItemId;
        this.answer = answer;
    }

    public static FormItemAnswer create(Long formItemId, String answer) {
        return new FormItemAnswer(formItemId, answer);
    }

    public void assignApplication(RecruitmentApplication application) {
        this.application = application;
    }
}
