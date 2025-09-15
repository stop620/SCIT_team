package com.hanzo.mochilearn.dto.quiz;

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

    private List<String> shuffleAnswer; // 정답 문장 배열 (SCRAMBLE)
    private List<String> blankAnswer; // 빈칸의 정답 단어들 (BLANK)
    private String choiceAnswer; // 정답 문장 (CHOICE)
}
