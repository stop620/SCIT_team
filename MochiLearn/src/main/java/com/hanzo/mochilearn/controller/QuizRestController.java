package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.QuizDTO;
import com.hanzo.mochilearn.dto.QuizResponseDTO;
import com.hanzo.mochilearn.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuizRestController {

    private final QuizService quizService;

    @GetMapping("/api/quizzes")
    public List<QuizResponseDTO> getQuizzes(@RequestParam int level){

        List<QuizResponseDTO> quizList = quizService.makeQuiz(level, 1);

        log.debug("[quiz controller] make quiz list: {}", quizList);

        return quizList;
    }
}
