package com.hanzo.mochilearn.dto.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {

    private int quizId;

    @JsonProperty("isCorrect")
    private boolean isCorrect;

    private QuizResponseDTO quiz;

    private List<String> userAnswer;

    private int level;
}
