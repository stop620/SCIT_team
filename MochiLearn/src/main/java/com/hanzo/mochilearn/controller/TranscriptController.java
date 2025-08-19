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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
public class TranscriptController {

    @Value("${gemini.api.key}")
    private String apiKey;

    // JSON 구조에 맞춰 파싱할 데이터를 담을 클래스 (내부 정적 클래스로 선언)
    @Getter
    private static class TranslationResponse {
        private String japanese;
        private String korean;

    }

    /**
     * yt-dlp를 최적화하여 필요한 구간만 다운로드하고, AI를 통해 텍스트로 변환합니다.
     * 실행 환경에 yt-dlp와 ffmpeg가 설치되고 시스템 PATH에 등록되어 있어야 합니다.
     */
    @PostMapping("/api/transcribe")
    @Async
    public CompletableFuture<ResponseEntity<?>> getTranscript(@RequestBody TranscriptRequestDTO requestDTO) {
        log.debug("getTranscript requestDTO: {}", requestDTO);
        return CompletableFuture.supplyAsync(() -> {
            Path tempDir = null;
            try {
                tempDir = Files.createTempDirectory("youtube-audio-");
                File slicedAudioFile = downloadAndSliceAudioStream(tempDir, requestDTO.getUrl(), requestDTO.getStart(), requestDTO.getEnd());

                // AI 호출 결과로 자막 리스트(DTO)를 받음
                List<TranscriptResponseDTO> transcription = callGeminiApi(slicedAudioFile);

                // 성공 시 자막 리스트를 그대로 응답
                return ResponseEntity.ok(transcription);

            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "스크립트 변환 중 오류 발생: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            } finally {
                if (tempDir != null) {
                    try {
                        Files.walk(tempDir)
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * yt-dlp와 ffmpeg를 함께 사용하여 지정된 시간 구간의 오디오만 mp3 파일로 다운로드합니다.
     */
    private File downloadAndSliceAudioStream(Path tempDir, String url, float startTime, float endTime) throws IOException, InterruptedException {
        File outputFile = tempDir.resolve("sliced_audio.mp3").toFile();
        float duration = endTime - startTime;

        // ffmpeg에 전달할 인자 설정: "-ss [시작시간] -t [지속시간]"
        String ffmpegArgs = String.format("-ss %.3f -t %.3f", startTime, duration);

        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-f", "bestaudio", // bestaudio를 선택하고 ffmpeg이 변환하도록 맡김
                "-x", // --extract-audio의 단축 옵션
                "--audio-format", "mp3",
                "--postprocessor-args", ffmpegArgs, // ffmpeg에 자르기 옵션 전달
                "-o", outputFile.getAbsolutePath(), // --output의 단축 옵션
                url);

        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("yt-dlp: " + line);
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp 프로세스 실행 실패 (종료 코드: " + exitCode + "). 출력:\n" + output.toString());
        }
        return outputFile;
    }

    /**
     * Gemini API를 호출하여 오디오 파일을 텍스트로 변환합니다.
     */
    private List<TranscriptResponseDTO> callGeminiApi(File audioFile) throws IOException {
        /*GenerativeModel model = new GenerativeModel("gemini-1.5-flash", apiKey);
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        Content content = new Content.Builder()
                .addPart(Part.fromMimeType(audioBytes, "audio/mpeg"))
                .addPart(Part.fromText("이 오디오를 듣고 내용을 한국어 텍스트로 정확하게 작성해줘."))
                .build();

        GenerateContentResponse response = model.generateContent(content);
        return response.getText();*/

        log.debug("call gemini api...");

        Client client = Client.builder().apiKey(apiKey).build();
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        Content content =Content.fromParts(
                Part.fromText("Transcribe this audio file into Japanese and Korean. " +
                        "If multiple lines appear in the file, short lines such as oh and ah may be omitted. " +
                        "All answers, whether one or more, are in JSON format " +
                        "{index: order, time: time from start time (seconds to 3 decimal places only), japanese: Japanese, korean: Korean}."),
                Part.fromBytes(audioBytes, "audio/mp3")
        );

        log.debug("[send to Gemini] contents: {}", content);




        GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash", content, null);

        String jsonResponse = response.text();

        if (jsonResponse.startsWith("```json")) {
            jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).trim();
        } else if (jsonResponse.startsWith("```")) {
            jsonResponse = jsonResponse.substring(3, jsonResponse.length() - 3).trim();
        }

        log.debug("[Gemini response] response: {}", jsonResponse);

        Gson gson = new Gson();

        // JSON 배열을 List<TranscriptItemDto> 타입으로 변환하기 위한 설정
        Type listType = new TypeToken<ArrayList<TranscriptResponseDTO>>() {}.getType();

        log.debug("[Gemini response] response: {}", listType);

        List<TranscriptResponseDTO> list = gson.fromJson(jsonResponse, listType);

        log.debug("[Gemini response] response: {}", list);

        // JSON 문자열을 DTO 리스트 객체로 파싱하여 반환
        return list;

    }
}
