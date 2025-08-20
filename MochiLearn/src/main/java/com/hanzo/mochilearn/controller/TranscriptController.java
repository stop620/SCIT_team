package com.hanzo.mochilearn.controller;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hanzo.mochilearn.dto.TranscriptRequestDTO;
import com.hanzo.mochilearn.dto.TranscriptResponseDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
public class TranscriptController {

    // application.properties에서 쉼표로 구분된 키 목록을 List<String>으로 주입받습니다.
    @Value("#{'${gemini.api.keys}'.split(',')}")
    private List<String> apiKeys;

    // 진행 중인 작업의 상태와 결과를 저장하는 스레드 안전한 Map
    private final Map<String, JobStatusDto> jobStatuses = new ConcurrentHashMap<>();

    // JSON 구조에 맞춰 파싱할 데이터를 담을 클래스 (내부 정적 클래스로 선언)
    @Getter
    private static class TranslationResponse {
        private String japanese;
        private String korean;

    }

    /** 작업의 상태와 결과(또는 에러)를 담는 DTO */
    public static class JobStatusDto {
        private String status; // "PROCESSING", "COMPLETED", "FAILED"
        private List<TranscriptResponseDTO> result;
        private String error;

        // 생성자, Getter, Setter
        public JobStatusDto(String status) { this.status = status; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<TranscriptResponseDTO> getResult() { return result; }
        public void setResult(List<TranscriptResponseDTO> result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * 1. 작업 요청 API: 변환 작업을 시작하고 즉시 작업 ID를 반환합니다.
     */
    @PostMapping("/api/transcribe/start")
    public ResponseEntity<Map<String, String>> startTranscription(@RequestBody TranscriptRequestDTO requestDto) {
        String jobId = UUID.randomUUID().toString();
        jobStatuses.put(jobId, new JobStatusDto("PROCESSING"));

        // 비동기적으로 실제 작업 수행
        processTranscription(jobId, requestDto);

        Map<String, String> response = new HashMap<>();
        response.put("jobId", jobId);
        return ResponseEntity.accepted().body(response); // HTTP 202 Accepted
    }

    /**
     * 2. 결과 확인 API: 작업 ID를 사용하여 현재 상태나 최종 결과를 조회합니다.
     */
    @GetMapping("/api/transcribe/status/{jobId}")
    public ResponseEntity<JobStatusDto> getTranscriptionStatus(@PathVariable String jobId) {
        JobStatusDto status = jobStatuses.get(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * 실제 변환 작업을 수행하는 비동기 메소드.
     * 작업이 완료되거나 실패하면 jobStatuses Map을 업데이트합니다.
     */
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

    /**
     * yt-dlp의 출력을 ffmpeg의 입력으로 직접 파이핑하여 메모리 내에서 스트림을 처리합니다.
     * 이 방식은 디스크 I/O를 최소화하여 매우 빠릅니다.
     */
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

    /**
     * Gemini API를 호출하여 오디오 파일을 텍스트로 변환합니다.
     */
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
    }
}
