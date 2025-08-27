package com.hanzo.mochilearn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponseDTO {

    private int quizId;
    private QuizType quizType;
    private String korean;

    // SCRAMBLE 타입에서 사용
    private List<String> shuffledSentence;

    // BLANK 타입에서 사용
    private List<String> blankSentence;
    private List<String> blankChoices;

    // CHOICE 타입에서 사용
    private List<String> choiceSentences;
}
