package com.hanzo.mochilearn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {

    private int quizId;
    private QuizType quizType;
    private boolean isCorrect;
    private List<String> correctAnswer;
    private List<String> userAnswer;
    private String korean;

}
