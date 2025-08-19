package com.hanzo.mochilearn.controller;

/*import com.google.genai.generativeai.GenerativeModel;
import com.google.genai.type.Content;
import com.google.genai.type.GenerateContentResponse;
import com.google.genai.type.Part;*/
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class TranscriptController {

/*    @Value("${gemini.api.key}")
    private String apiKey;*/

    /**
     * 비동기적으로 유튜브 음성을 텍스트로 변환합니다.
     * 이 코드는 실행 환경(서버)에 yt-dlp와 ffmpeg가 설치되고
     * 시스템 PATH에 등록되어 있는 것을 전제로 합니다.
     */
    @PostMapping("/api/transcribe")
    @Async
    public CompletableFuture<ResponseEntity<Map<String, String>>> getTranscript(@RequestBody Map<String, Object> requestBody) {
        String youtubeUrl = (String) requestBody.get("url");
        Number startTimeNum = (Number) requestBody.get("start");
        Number endTimeNum = (Number) requestBody.get("end");
        float startTime = startTimeNum.floatValue();
        float endTime = endTimeNum.floatValue();

        return CompletableFuture.supplyAsync(() -> {
            Path tempDir = null;
            try {
                // 1. 작업을 위한 임시 디렉토리 생성
                tempDir = Files.createTempDirectory("youtube-audio-");

                // 2. 오디오 다운로드 및 mp3 변환 (yt-dlp가 PATH의 ffmpeg 사용)
                File fullAudioFile = downloadAudioStream(tempDir, youtubeUrl);

                // 3. 오디오 파일 자르기 (JAVE가 PATH의 ffmpeg 사용)
                File slicedAudioFile = sliceAudioFile(tempDir, fullAudioFile, startTime, endTime);

                // 4. Gemini API로 텍스트 변환
                //String transcription = callGeminiApi(slicedAudioFile);

                Map<String, String> result = new HashMap<>();
                //result.put("transcript", transcription);
                return ResponseEntity.ok(result);

            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("error", "스크립트 변환 중 오류 발생: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            } finally {
                // 5. 작업 완료 후 임시 디렉토리 정리
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
     * yt-dlp를 실행하여 오디오를 다운로드하고 mp3로 변환합니다.
     * 시스템 PATH에 등록된 ffmpeg을 자동으로 사용합니다.
     */
    private File downloadAudioStream(Path tempDir, String url) throws IOException, InterruptedException {
        File downloadedFile = tempDir.resolve("audio.mp3").toFile();
        ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp",
                "-f", "bestaudio[ext=m4a]",
                "--extract-audio",
                "--audio-format", "mp3",
                "--output", downloadedFile.getAbsolutePath(),
                url);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp 프로세스 실행 실패 (종료 코드: " + exitCode + "). 출력:\n" + output.toString());
        }
        return downloadedFile;
    }

    /**
     * JAVE를 사용하여 오디오 파일을 자릅니다.
     * 시스템 PATH에 등록된 ffmpeg을 자동으로 감지하여 사용합니다.
     */
    private File sliceAudioFile(Path tempDir, File sourceFile, float startTime, float endTime) throws EncoderException {
        File slicedFile = tempDir.resolve("sliced.mp3").toFile();

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(128000);
        audio.setChannels(1);
        audio.setSamplingRate(16000);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("mp3");
        attrs.setAudioAttributes(audio);
        attrs.setDuration(endTime - startTime);
        attrs.setOffset(startTime);

        Encoder encoder = new Encoder(); // 기본 생성자로 시스템 PATH의 ffmpeg 사용
        MultimediaObject input = new MultimediaObject(sourceFile);
        encoder.encode(input, slicedFile, attrs);
        return slicedFile;
    }

    /**
     * Gemini API를 호출하여 오디오 파일을 텍스트로 변환합니다.
     */
    /*private String callGeminiApi(File audioFile) throws IOException {
        GenerativeModel model = new GenerativeModel("gemini-1.5-flash", apiKey);
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        Content content = new Content.Builder()
                .addPart(Part.fromMimeType(audioBytes, "audio/mpeg"))
                .addPart(Part.fromText("이 오디오를 듣고 내용을 한국어 텍스트로 정확하게 작성해줘."))
                .build();

        GenerateContentResponse response = model.generateContent(content);
        return response.getText();
    }*/
}
