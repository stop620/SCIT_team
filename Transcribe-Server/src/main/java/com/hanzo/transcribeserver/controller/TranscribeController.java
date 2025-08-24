package com.hanzo.transcribeserver.controller;


import com.hanzo.transcribeserver.service.TranscribeService;
import com.hanzo.transcribeserver.dto.RequestDTO;
import com.hanzo.transcribeserver.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TranscribeController {

    private final TranscribeService transcribeService;

    // URL과 시간 정보를 받아 자막 추출 결과를 반환
    @PostMapping("/api/transcribe")
    public ResponseEntity<?> transcribe(@RequestBody RequestDTO requestDto) {

        log.info("[메인 서버 요청 수신] : url={}, start={}, end={}", requestDto.getUrl(), requestDto.getStart(), requestDto.getEnd());
        log.info("[작업 시작 스레드] : {}", Thread.currentThread().getName());

        try {

            List<ResponseDTO> transcription = transcribeService.processTranscription(requestDto.getUrl(), requestDto.getStart(), requestDto.getEnd());

            log.info("자막 추출 성공. 결과를 반환합니다.");
            log.debug("[작업 종료 스레드] : {}", Thread.currentThread().getName());

            return ResponseEntity.ok(transcription);

        } catch (Exception e) {
            log.error("AI 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AI 처리 중 오류 발생: " + e.getMessage()));
        }
    }
}
