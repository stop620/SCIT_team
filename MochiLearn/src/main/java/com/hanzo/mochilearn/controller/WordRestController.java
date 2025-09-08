package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.ApiResponse;
import com.hanzo.mochilearn.dto.BookDTO;
import com.hanzo.mochilearn.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class WordRestController {

    private final BookService bookService;

    @GetMapping("/api/word/wordbook")
    public ResponseEntity<ApiResponse<List<BookDTO>>> wordBook(@RequestParam("memberId") Integer memberId) {

        List<BookDTO> books = bookService.getBooks(memberId);

        return ResponseEntity.ok(ApiResponse.success("단어장 로드 성공", books));
    }
}
