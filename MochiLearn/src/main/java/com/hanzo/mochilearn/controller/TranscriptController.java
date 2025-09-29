package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.JobStatusDTO;
import com.hanzo.mochilearn.dto.TranscriptRequestDTO;
import com.hanzo.mochilearn.dto.TranscriptResponseDTO;
import com.hanzo.mochilearn.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TranscriptController {

    private final TranscriptService transcriptService;

    // 1. 작업 요청 API - 변환 작업을 시작하고 작업 ID를 반환
    @PostMapping("/api/transcribe/start")
    public ResponseEntity<Map<String, String>> startTranscription(@RequestBody TranscriptRequestDTO requestDto) {

        String jobId = transcriptService.createJob();

        // 비동기로 실제 처리 요청
        transcriptService.requestTranscribe(jobId, requestDto);

        // 작업 시작한 id만 응답
        Map<String, String> response = new HashMap<>();
        response.put("jobId", jobId);

        return ResponseEntity.accepted().body(response); // HTTP 202 Accepted
    }

    // 2. 결과 확인 API - jobId로 결과 조회
    @GetMapping("/api/transcribe/status/{jobId}")
    public ResponseEntity<JobStatusDTO> getTranscriptionStatus(@PathVariable("jobId") String jobId) {

        // jobId로 현재 상태 반환
        JobStatusDTO status = transcriptService.getJobStatus(jobId);

        if (status == null) { //작업 없음
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }




}
