package com.hanzo.transcribeserver.controller;

import com.hanzo.transcribeserver.dict.JmdictWord;
import com.hanzo.transcribeserver.dto.ApiResponse;
import com.hanzo.transcribeserver.dto.DictionaryResponseDTO;
import com.hanzo.transcribeserver.service.JmdictService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@CrossOrigin(origins = "http://localhost:9000")
@RestController
public class JmdictController {

    @Autowired
    private JmdictService jmdictService;

    @GetMapping("/api/dictionary/search")
    public ResponseEntity<ApiResponse<DictionaryResponseDTO>> searchWord(@RequestParam("query") String query) {

        log.debug("searchWord query: {}", query);
        Optional<DictionaryResponseDTO> translatedWord = jmdictService.findWord(query);

        if(translatedWord.isPresent()) {
            log.debug("translatedWord: {}", translatedWord.get());

            return ResponseEntity.ok(ApiResponse.success("단어 검색 성공", translatedWord.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.fail("NOT_FOUND", "단어를 찾을 수 없습니다."));
        }
    }
}
