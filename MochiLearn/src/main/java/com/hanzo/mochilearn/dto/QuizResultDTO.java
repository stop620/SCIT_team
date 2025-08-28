package com.hanzo.mochilearn.dto;

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

    private boolean isCorrect;

    @JsonProperty("quiz")
    private List<QuizResponseDTO> quizList;

    private List<String> userAnswer;


}
