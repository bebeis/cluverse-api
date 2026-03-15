package cluverse.recruitment.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FormItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "form_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @Column(nullable = false)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private FormItemQuestionType questionType;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "json")
    private List<String> options;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private FormItem(String question,
                     FormItemQuestionType questionType,
                     boolean isRequired,
                     List<String> options,
                     int displayOrder) {
        this.question = question;
        this.questionType = questionType;
        this.isRequired = isRequired;
        this.options = options == null ? List.of() : List.copyOf(options);
        this.displayOrder = displayOrder;
    }

    public static FormItem create(String question,
                                  FormItemQuestionType questionType,
                                  boolean isRequired,
                                  List<String> options,
                                  int displayOrder) {
        return new FormItem(question, questionType, isRequired, options, displayOrder);
    }

    public void assignRecruitment(Recruitment recruitment) {
        this.recruitment = recruitment;
    }
}
