package com.hanzo.mochilearn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "quiz_answer")
public class QuizAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Integer answerId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "attempt_id")
    private QuizAttemptEntity attempt;

    @Column(name = "quiz_id")
    private Integer quizId;

    private Integer questionNo;

    @Column(name = "user_answer")
    private String userAnswer;

    @Column(name = "is_correct")
    private boolean isCorrect;
}
