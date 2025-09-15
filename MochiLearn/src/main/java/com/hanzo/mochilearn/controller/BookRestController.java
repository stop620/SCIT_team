package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.ApiResponse;
import com.hanzo.mochilearn.dto.word.BookDTO;
import com.hanzo.mochilearn.security.AuthenticatedUser;
import com.hanzo.mochilearn.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class BookRestController {

    private final BookService bookService;

    @GetMapping("/api/wordbook/list")
    public ResponseEntity<ApiResponse<List<BookDTO>>> wordBook(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        if(authenticatedUser == null) {
            return ResponseEntity.ok(ApiResponse.fail( "fail","단어장 로드 실패(로그인 필요)"));
        }

        Integer memberId = authenticatedUser.getMemberId();
        try {
            List<BookDTO> books = bookService.getBooks(memberId);
            return ResponseEntity.ok(ApiResponse.success("단어장 로드 성공", books));

        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/api/wordbook/create")
    public ResponseEntity<ApiResponse<String>> createBook(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                           @RequestBody BookDTO bookDTO) {

        log.debug("[단어장 추가 요청]: {}", bookDTO);
        bookService.createWordBook(bookDTO, authenticatedUser.getMemberId());

        return ResponseEntity.ok(ApiResponse.success("단어장 추가 성공", "success"));
    }

    @DeleteMapping("/api/wordbook/delete")
    public ResponseEntity<ApiResponse<String>> removeWord(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                          @RequestParam(name = "bookId") Integer bookId) {
        log.debug("[단어장 삭제 요청]: 단어장ID {}", bookId);
        try {
            bookService.removeBook(bookId, authenticatedUser.getMemberId());
            return ResponseEntity.ok(ApiResponse.success("단어장 삭제 성공", "success"));

        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail("error", "단어장 삭제 실패"));
        }
    }
}
