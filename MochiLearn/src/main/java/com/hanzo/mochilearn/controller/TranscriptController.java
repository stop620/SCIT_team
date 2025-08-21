package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.JobStatusDTO;
import com.hanzo.mochilearn.dto.TranscriptRequestDTO;
import com.hanzo.mochilearn.dto.TranscriptResponseDTO;
import lombok.Data;
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
@RestController
public class TranscriptController {

    // application.properties에서 Python 모듈의 주소 주입
    @Value("${python.service.url}")
    private String pythonServiceUrl;

    // HTTP 요청을 보내기 위한 RestTemplate
    private final RestTemplate restTemplate = new RestTemplate();

    // 진행 중인 작업의 상태와 결과를 저장하는 Map
    private final Map<String, JobStatusDTO> jobStatuses = new ConcurrentHashMap<>();



    // 1. 작업 요청 API - 변환 작업을 시작하고 작업 ID를 반환
    @PostMapping("/api/transcribe/start")
    public ResponseEntity<Map<String, String>> startTranscription(@RequestBody TranscriptRequestDTO requestDto) {

        String jobId = UUID.randomUUID().toString();
        jobStatuses.put(jobId, new JobStatusDTO("PROCESSING"));

        // 비동기로 실제 처리 요청
        processTranscription(jobId, requestDto);

        Map<String, String> response = new HashMap<>();
        response.put("jobId", jobId);

        return ResponseEntity.accepted().body(response); // HTTP 202 Accepted
    }

    // 2. 결과 확인 API - jobId로 결과 조회
    @GetMapping("/api/transcribe/status/{jobId}")
    public ResponseEntity<JobStatusDTO> getTranscriptionStatus(@PathVariable String jobId) {

        JobStatusDTO status = jobStatuses.get(jobId);

        if (status == null) {
            return ResponseEntity.notFound().build(); //작업 없음
        }

        return ResponseEntity.ok(status);
    }

    // 실제 변환 작업을 수행
    // Python 모듈에게 요청하고 결과를 받아 jobStatuses Map을 업데이트
    @Async
    public void processTranscription(String jobId, TranscriptRequestDTO requestDto) {

        try {
            // Python 모듈에 보낼 요청문 생성
            Map<String, Object> pythonRequest = new HashMap<>();

            pythonRequest.put("url", requestDto.getUrl());
            pythonRequest.put("start", requestDto.getStart());
            pythonRequest.put("end", requestDto.getEnd());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pythonRequest, headers);

            log.info("파이썬 모듈에게 요청전송 (jobId: {})", jobId);

            // Python 서버에 POST 요청 전송
            // Python 서버가 JSON 배열을 반환하므로 ParameterizedTypeReference를 사용
            ResponseEntity<List<TranscriptResponseDTO>> response = restTemplate.exchange(
                    pythonServiceUrl + "/api/transcribe",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<TranscriptResponseDTO> transcription = response.getBody();
            log.info("Received response from Python AI module for jobId: {}", jobId);

            // 작업 성공 시 상태 업데이트
            JobStatusDTO finalStatus = jobStatuses.get(jobId);
            finalStatus.setStatus("COMPLETED");
            finalStatus.setResult(transcription);

        } catch (Exception e) {
            e.printStackTrace();
            // 작업 실패 시 상태 업데이트
            JobStatusDTO finalStatus = jobStatuses.get(jobId);
            finalStatus.setStatus("FAILED");
            finalStatus.setError("스크립트 변환 중 오류 발생: " + e.getMessage());
        }
    }

    // 파이썬 모듈로 이전
    /*
    // application.properties에서 쉼표로 구분된 키 목록을 List<String>으로 주입받습니다.
    @Value("#{'${gemini.api.keys}'.split(',')}")
    private List<String> apiKeys;

    /**
     * 실제 변환 작업을 수행하는 비동기 메소드.
     * 작업이 완료되거나 실패하면 jobStatuses Map을 업데이트합니다.
     *//*
    @Async
    public void processTranscription(String jobId, TranscriptRequestDTO requestDto) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("youtube-audio-");
            File slicedAudioFile = downloadAndSliceAudioStream(tempDir, requestDto.getUrl(), requestDto.getStart(), requestDto.getEnd());
            List<TranscriptResponseDTO> transcription = callGeminiApi(slicedAudioFile);

            // 작업 성공 시 상태 업데이트
            JobStatusDto finalStatus = jobStatuses.get(jobId);
            finalStatus.setStatus("COMPLETED");
            finalStatus.setResult(transcription);

        } catch (Exception e) {
            e.printStackTrace();
            // 작업 실패 시 상태 업데이트
            JobStatusDto finalStatus = jobStatuses.get(jobId);
            finalStatus.setStatus("FAILED");
            finalStatus.setError("스크립트 변환 중 오류 발생: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                // ... 임시 디렉토리 삭제 로직 ...
            }
        }
    }

    *//**
     * yt-dlp의 출력을 ffmpeg의 입력으로 직접 파이핑하여 메모리 내에서 스트림을 처리합니다.
     * 이 방식은 디스크 I/O를 최소화하여 매우 빠릅니다.
     *//*
    private File downloadAndSliceAudioStream(Path tempDir, String url, float startTime, float endTime) throws IOException, InterruptedException {
        File outputFile = tempDir.resolve("sliced_audio.mp3").toFile();

        ProcessBuilder ytDlpProcessBuilder = new ProcessBuilder(
                "yt-dlp", "-f", "bestaudio", "-o", "-", url);
        ProcessBuilder ffmpegProcessBuilder = new ProcessBuilder(
                "ffmpeg", "-ss", String.format("%.3f", startTime), "-i", "-",
                "-t", String.format("%.3f", endTime - startTime),
                "-c:a", "libmp3lame", "-b:a", "128k", "-f", "mp3",
                outputFile.getAbsolutePath());

        Process ytDlpProcess = null;
        Process ffmpegProcess = null;

        try {
            ytDlpProcess = ytDlpProcessBuilder.start();
            ffmpegProcess = ffmpegProcessBuilder.start();

            final Process finalYtDlpProcess = ytDlpProcess;
            final Process finalFfmpegProcess = ffmpegProcess;

            // yt-dlp의 출력을 ffmpeg의 입력으로 연결하는 스레드
            Thread pipeThread = new Thread(() -> {
                try (InputStream input = finalYtDlpProcess.getInputStream();
                     OutputStream output = finalFfmpegProcess.getOutputStream()) {
                    input.transferTo(output);
                } catch (IOException e) {
                    System.out.println("Pipe thread I/O Exception (expected on success): " + e.getMessage());
                }
            });

            // yt-dlp의 에러 스트림을 읽어 교착 상태를 방지하는 스레드
            StringBuilder ytDlpError = new StringBuilder();
            Thread ytDlpErrorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(finalYtDlpProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("yt-dlp-error: " + line);
                        ytDlpError.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            pipeThread.start();
            ytDlpErrorThread.start();

            StringBuilder ffmpegOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("ffmpeg: " + line);
                    ffmpegOutput.append(line).append("\n");
                }
            }

            int ffmpegExitCode = ffmpegProcess.waitFor();
            int ytExitCode = ytDlpProcess.waitFor();
            pipeThread.join();
            ytDlpErrorThread.join();

            if (ffmpegExitCode != 0) {
                throw new IOException("ffmpeg 프로세스가 비정상적으로 종료되었습니다 (코드: " + ffmpegExitCode + "). 출력:\n" + ffmpegOutput);
            }
            if (ytExitCode != 0) {
                // ffmpeg이 성공했더라도 yt-dlp 에러가 있다면 로그를 남기는 것이 좋습니다.
                System.err.println("yt-dlp 프로세스가 비정상적으로 종료되었지만 ffmpeg 작업은 성공했을 수 있습니다. yt-dlp 출력:\n" + ytDlpError);
            }

            return outputFile;

        } finally {
            if (ytDlpProcess != null) ytDlpProcess.destroyForcibly();
            if (ffmpegProcess != null) ffmpegProcess.destroyForcibly();
        }
    }

    *//**
     * Gemini API를 호출하여 오디오 파일을 텍스트로 변환합니다.
     *//*
    private List<TranscriptResponseDTO> callGeminiApi(File audioFile) throws IOException {

        log.debug("call gemini api...");

        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        Content content =Content.fromParts(
                Part.fromText("Transcribe this audio file into Japanese and Korean. " +
                        "If multiple lines appear in the file, short lines such as oh and ah may be omitted. " +
                        "All answers, whether one or more, are in JSON format " +
                        "{index: order, time: time from start time (seconds to 3 decimal places only), japanese: Japanese, korean: Korean}."),
                Part.fromBytes(audioBytes, "audio/mp3")
        );

        log.debug("[send to Gemini] contents: {}", content);


        for(String currentApiKey : apiKeys) {
            try {
                Client client = Client.builder().apiKey(currentApiKey).build();
                GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash", content, null);
                String rawResponse = response.text();

                // 정상적인 응답을 받으면, 파싱하고 결과를 즉시 반환
                if (rawResponse != null && !rawResponse.isBlank()) {
                    if (rawResponse.startsWith("```json")) {
                        rawResponse = rawResponse.substring(7, rawResponse.length() - 3).trim();
                    } else if (rawResponse.startsWith("```")) {
                        rawResponse = rawResponse.substring(3, rawResponse.length() - 3).trim();
                    }
                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<TranscriptResponseDTO>>() {}.getType();
                    log.debug("[Gemini response] response: {}", rawResponse);
                    return gson.fromJson(rawResponse, listType);
                }
                // 응답이 비어있으면 다음 키로 넘어감
                System.out.println("Received empty response, trying next key...");

            } catch (Exception e) {
            // API 호출 중 다른 예외 발생 시 다음 키로 넘어감
            System.err.println("API call failed for a key: " + e.getMessage());
        }
    }
    // 모든 키를 시도했지만 실패한 경우
        throw new IOException("모든 API 키를 사용했지만 Gemini API 호출에 실패했습니다. API 키 할당량을 확인해주세요.");
    }*/
}
