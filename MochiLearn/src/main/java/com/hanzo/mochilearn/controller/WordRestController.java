package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.ApiResponse;
import com.hanzo.mochilearn.dto.BookDTO;
import com.hanzo.mochilearn.dto.TranslateDTO;
import com.hanzo.mochilearn.security.AuthenticatedUser;
import com.hanzo.mochilearn.service.BookService;
import com.hanzo.mochilearn.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class WordRestController {

    private final WordService wordService;

    @PostMapping("/api/word/translate")
    public ResponseEntity<ApiResponse<List<String>>> translateWord(@RequestBody TranslateDTO translateDTO) {

        log.debug("[번역할 단어 목록]: {}, context: {}", translateDTO.getWordList(), translateDTO.getContext());

        List<String> result = wordService.translate(translateDTO);

        return ResponseEntity.ok(ApiResponse.success("단어 목록 번역 성공", result));
    }
}
