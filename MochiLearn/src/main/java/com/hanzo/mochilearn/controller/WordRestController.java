package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.*;
import com.hanzo.mochilearn.dto.word.WordDTO;
import com.hanzo.mochilearn.security.AuthenticatedUser;
import com.hanzo.mochilearn.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
public class WordRestController {

    private final WordService wordService;

    @PostMapping("/api/word/save")
    public ResponseEntity<ApiResponse<Integer>> saveWord(@RequestBody WordDTO wordDTO) {

        log.debug("[저장할 단어]: {}", wordDTO);

        Integer bookId = wordService.save(wordDTO);
        if(bookId != -1) {
            return ResponseEntity.ok(ApiResponse.success("단어 저장 성공", bookId));

        } else {
            return ResponseEntity.ok(ApiResponse.fail("-1", "이미 저장된 단어"));
        }

    }

    @DeleteMapping("/api/word/delete")
    public ResponseEntity<ApiResponse<String>> removeWord(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                          @RequestParam(name = "bookId") Integer bookId,
                                                          @RequestParam(name = "wordId") Integer wordId) {
        log.debug("[단어 삭제 요청]: 단어장 {}에서 단어 {}삭제", bookId, wordId);
        try {
            wordService.removeWord(bookId, wordId, authenticatedUser.getMemberId());
            return ResponseEntity.ok(ApiResponse.success("단어 삭제 성공", "success"));

        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail("error", "단어 삭제 실패"));
        }
    }
}
