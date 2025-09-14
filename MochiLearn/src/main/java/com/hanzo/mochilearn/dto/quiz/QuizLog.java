package com.hanzo.mochilearn.dto.quiz;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class QuizLog {

    public QuizLog(){
        this.userAnswers = new ArrayList<>();
    }
    private Integer sessionId;
    private int level;
    private String quizData;

    private Integer attemptId;
    private int attemptNo;
    private int score;
    private LocalDateTime attemptDate;

    private List<UserAnswer> userAnswers;

    @Data
    private static class UserAnswer {
        private int questionNo;
        String answer;
    }

    public void addUserAnswer(int no, String answer) {
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.questionNo = no;
        userAnswer.answer = answer;
        userAnswers.add(userAnswer);
    }
}
