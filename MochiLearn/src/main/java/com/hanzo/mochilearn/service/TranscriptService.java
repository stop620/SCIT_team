package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.JobStatusDTO;
import com.hanzo.mochilearn.dto.TranscriptRequestDTO;
import com.hanzo.mochilearn.dto.TranscriptResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TranscriptService {

    // application.properties에서 transcribe server 주소 주입
    @Value("${transcribe.server.url}")
    private String transcribeServerUrl;

    // HTTP 요청을 보내기 위한 RestTemplate
    private final RestTemplate restTemplate = new RestTemplate();

    // 진행 중인 작업의 상태와 결과를 저장하는 Map
    private final Map<String, JobStatusDTO> jobStatuses = new ConcurrentHashMap<>();

    public String createJob() {
        String jobId = UUID.randomUUID().toString();
        jobStatuses.put(jobId, new JobStatusDTO("PROCESSING"));

        log.debug("[새로운 작업 생성] jobId: {}", jobId);

        return jobId;
    }

    public JobStatusDTO getJobStatus(String jobId) {

        log.debug("[반환된 작업 id] jobId: {}", jobId);

        return jobStatuses.get(jobId);
    }

    // transcribe server에 요청하고 결과를 받는 비동기 메소드
    @Async
    public void requestTranscribe(String jobId, TranscriptRequestDTO requestDto) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TranscriptRequestDTO> entity = new HttpEntity<>(requestDto, headers);

            String url = transcribeServerUrl + "/api/transcribe";
            log.info("AI 서버에 작업 요청 전송 (Job ID: {}): URL={}", jobId, url);

            // RestTemplate을 사용하여 transcribe 서버에 POST 요청 전송
            ResponseEntity<List<TranscriptResponseDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            // 작업 성공 시 상태 업데이트
            JobStatusDTO finalStatus = jobStatuses.get(jobId);
            finalStatus.setStatus("COMPLETED");
            finalStatus.setResult(response.getBody());

            log.debug("AI 서버로부터 작업 결과 수신 완료 (Job ID: {})", jobId);
            log.debug("[수신한 응답] : {}", response);

        } catch (Exception e) {
            log.error("AI 서버 통신 중 오류 발생 (Job ID: {})", jobId, e);

            // 작업 실패 시 상태 업데이트
            JobStatusDTO finalStatus = jobStatuses.get(jobId);
            finalStatus.setStatus("FAILED");
            finalStatus.setError("AI 서버 통신 중 오류 발생: " + e.getMessage());
        }
    }
}
