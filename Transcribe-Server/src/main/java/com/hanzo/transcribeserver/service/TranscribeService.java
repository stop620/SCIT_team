package com.hanzo.transcribeserver.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hanzo.transcribeserver.dto.GeminiResponseDTO;
import com.hanzo.transcribeserver.dto.SentenceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TranscribeService {

    // application.properties에서 키 목록을 List<String>으로 주입
    @Value("#{'${gemini.api.keys}'.split(',')}")
    private List<String> apiKeys;

    private final AtomicInteger apiKeyIndex = new AtomicInteger(0);
    // 동시에 최대 4개의 스레드만 API를 호출하도록 허용 (현재 api 키 갯수)
    private final Semaphore apiCallSemaphore = new Semaphore(4);

    public GeminiResponseDTO processTranscription(String url, float startTime, float endTime) throws IOException, InterruptedException {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("youtube-audio-");
            log.info("임시 디렉토리 생성: {}", tempDir);

            File slicedAudioFile = downloadAndSliceAudio(tempDir, url, startTime, endTime);

            return callGeminiApi(slicedAudioFile);

        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    log.info("임시 디렉토리 삭제 완료: {}", tempDir);
                } catch (IOException e) {
                    log.error("임시 디렉토리 삭제 실패: {}", tempDir, e);
                }
            }
        }
    }

    // yt-dlp 다운로드 하면서 ffmpeg로 필요한 구간만 처리


    public File downloadAndSliceAudio(Path tempDir, String url, float startTime, float endTime) throws IOException, InterruptedException {
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

    // Gemini API를 호출하여 오디오 파일을 텍스트로 변환
    public GeminiResponseDTO callGeminiApi(File audioFile) throws IOException, InterruptedException {

        log.debug("gemini api 호출 세마포어 대기중... (현재 스레드: {})", Thread.currentThread().getName());
        apiCallSemaphore.acquire();
        log.debug("gemini api 호출 세마포어 획득... (현재 스레드: {})", Thread.currentThread().getName());

        try {
            int startKeyIndex = apiKeyIndex.getAndIncrement();

            byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
            String prompt = "이 오디오 파일을 일본어와 한국어로 전사해줘. 구간 전체적인 난이도도 1~3레벨로 평가해줘 3이 고급수준이야." +
                    "감탄사 같은 표현은 생략해주세요. " +
                    "일본어 텍스트는 공백문자가 없어야합니다." +
                    "하나 이상의 답변은 모두 JSON 형식 transcriptions: [{index: 순서, time: 시작으로부터 해당 자막 시작시간(초), japanese: 일본어, korean: 한국어}, ...]. {level: 평균 난이도}" +
                    "추가로 문장 중에 퀴즈에 넣을만한 정도의 표현과 길이를 가진 문장은 별도로 quiz_sentences: [{japanese: 일본어, korean: 한국어, level: 난이도}, ...] 형식으로 주세요." +
                    "퀴즈의 일본어 텍스트는 조사의 뒤나 하나의 단어 뒤에서 구분해서 배열 형태로 나누어 주세요.";

            Content content =Content.fromParts(
                    Part.fromText(prompt),
                    Part.fromBytes(audioBytes, "audio/mpeg")
            );

            for(int i = 0; i < apiKeys.size(); i++) {
                int currentKeyIndex = (startKeyIndex + i) % apiKeys.size();
                String currentApiKey = apiKeys.get(currentKeyIndex);
                String keySubstring = currentApiKey.substring(Math.max(0, currentApiKey.length() - 4));


                try {
                    log.info("[Gemini API] 호출 시도 ({}/{})... Key: ...{}", i + 1, apiKeys.size(), keySubstring);
                    log.debug("[Gemini API] contents: {}", content);

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
                        log.debug("[Gemini Api] rawResponse: {}", rawResponse);
                        // 최상위 DTO인 GeminiResponseDto 타입으로 파싱합니다.
                        return gson.fromJson(rawResponse, GeminiResponseDTO.class);
                    }
                    // 응답이 비어있으면 다음 키로 재시도
                    log.warn("[Gemini Api] 빈 응답을 받았습니다. 다음 키로 재시도합니다.");
                } catch (Exception e) {
                    // 오류 발생 시 다음 키로 재시도
                    log.error("[Gemini Api] 오류 발생 (키: ...{}): {}. 다음 키로 재시도합니다.", keySubstring, e.getMessage());
                }
            }
            throw new IOException("[Gemini Api] 모든 키를 사용했지만 Gemini 호출에 실패했습니다.");
        } finally {
            apiCallSemaphore.release();
            log.debug("gemini api 호출 세마포어 반납... (현재 스레드: {})", Thread.currentThread().getName());
        }
    }
}
