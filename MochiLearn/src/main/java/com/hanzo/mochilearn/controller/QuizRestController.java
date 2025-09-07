package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.*;
import com.hanzo.mochilearn.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuizRestController {

    private final QuizService quizService;

    @GetMapping("/api/quiz")
    public List<QuizResponseDTO> getQuizzes(@RequestParam("level") int level){

        List<QuizResponseDTO> quizList = quizService.makeQuiz(level, 1);

        log.debug("[quiz controller] make quiz list: {}", quizList);

        return quizList;
    }

    @PostMapping("/api/quiz/saveResult")
    public ResponseEntity<ApiResponse<Integer>> saveQuiz(@RequestBody List<QuizResultDTO> quizResultDtoList){

        log.debug("[quiz controller] saveQuizResult: {}", quizResultDtoList);

        try {
            // quizResult 엔티티 생성 및 저장
            // TODO: 유저정보 생기면 로직 추가해야됨 지금은 1로 임의 저장
            Integer memberId = quizService.saveResult(quizResultDtoList, 1);

            return ResponseEntity.ok(ApiResponse.success("퀴즈 결과 저장 성공!", memberId));

        } catch (Exception e) {
            log.error("퀴즈 결과 저장 오류", e);
            // TODO: 오류 메시지 세분화 필요
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("퀴즈 결과 저장 실패.", null));
        }
    }
}
