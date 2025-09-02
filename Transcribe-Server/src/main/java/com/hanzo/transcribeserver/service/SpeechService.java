package com.hanzo.transcribeserver.service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hanzo.transcribeserver.dto.SpeechResponseDTO;
import com.hanzo.transcribeserver.dto.Word;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SpeechService {

    @Value("${azure.api.key}")
    private String azureKey;

    private static final String speechRegion = "koreacentral";

    // Gson 파싱용 내부 클래스
    private static class PronunciationAssessmentResponse {
        @SerializedName("NBest")
        private List<NBest> nBest;
    }
    private static class NBest {
        @SerializedName("Words")
        private List<JsonWord> words;
    }
    private static class JsonWord {
        @SerializedName("Word")
        private String word;
        @SerializedName("Duration")
        private Long duration;
        @SerializedName("PronunciationAssessment")
        private PronunciationAssessment pronAssessment;
    }
    private static class PronunciationAssessment {
        @SerializedName("ErrorType")
        private String errorType;
        @SerializedName("AccuracyScore")
        private Double accuracyScore;
    }
    // 내부 클래스 끝

    // 오디오 처리, ai 호출 결과 반환 메인 메소드
    public SpeechResponseDTO  pronunciationAssessment(MultipartFile file, String referenceText) throws IOException, InterruptedException, ExecutionException {

        //ffmpeg로 wav 파일로 변환
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("speech-audio-");
            log.info("임시 디렉토리 생성: {}", tempDir);
            File wavFile = convertAudio(tempDir, file);

            SpeechResponseDTO speechResponseDTO = callAzureApi(wavFile, referenceText);

            log.debug("speechResponseDTO: {}", speechResponseDTO);

            return speechResponseDTO;

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

    private File convertAudio(Path tempDir, MultipartFile originalFile) throws IOException, InterruptedException {

        File webmFile = tempDir.resolve("audio.webm").toFile();
        File wavFile = tempDir.resolve("audio.wav").toFile();

        log.debug("임시파일생성");

        try (FileOutputStream fos = new FileOutputStream(webmFile)) {
            fos.write(originalFile.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // FFmpeg 명령어 설정
        ProcessBuilder ffmpegProcessBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", webmFile.getAbsolutePath(), // 입력 파일
                "-ar", "16000",                 // 샘플링 레이트 16kHz
                "-ac", "1",                     // 오디오 채널 1 (모노)
                wavFile.getAbsolutePath()       // 출력 파일
        );
        log.debug("ffmpeg설정");

        // FFmpeg 프로세스를 실행하고 종료될 때까지 대기
        Process ffmpegProcess = null;
        try {
            ffmpegProcess = ffmpegProcessBuilder.start();
            int exitCode = ffmpegProcess.waitFor();
            log.debug("ffmpeg 종료코드: {}", exitCode);
            if (exitCode != 0) {
                // FFmpeg 변환 실패 시 에러 로그를 읽고 예외
                try (InputStream errorStream = ffmpegProcess.getErrorStream()) {
                    String error = new String(errorStream.readAllBytes());
                    throw new RuntimeException("FFmpeg 변환 실패. 에러 코드: " + exitCode + ", 에러 메시지: " + error);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (ffmpegProcess != null) {
                ffmpegProcess.destroyForcibly();
            }
        }

        log.debug("ffmpeg완료");

        return wavFile;
    }

    // azure speech service ai 호출
    private SpeechResponseDTO callAzureApi(File audioFile, String referenceText) throws ExecutionException, InterruptedException {

        // speech recognizer
        // api Key, service region 설정
        SpeechConfig config = SpeechConfig.fromSubscription(azureKey, speechRegion);
        String lang = "ja-JP";      // 일본어 설정
        AudioConfig audioInput = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());

        SpeechRecognizer recognizer = new SpeechRecognizer(config, lang, audioInput);

        // 발음평가 구성
        PronunciationAssessmentConfig pronunciationConfig = new PronunciationAssessmentConfig(referenceText,
                PronunciationAssessmentGradingSystem.HundredMark, PronunciationAssessmentGranularity.Word, true);


        pronunciationConfig.applyTo(recognizer);

        /**
         * 일반모드구성
         */
        // 단일 인식 시작
        SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get();

        if (result.getReason() != ResultReason.RecognizedSpeech) {
            log.warn("음성 인식 실패: {}", result.getReason());
            return new SpeechResponseDTO(result.getReason().toString());
        }

        String jString = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
        if (jString == null) {
            log.warn("인식 결과 JSON이 null입니다.");
            return new SpeechResponseDTO(result.getReason().toString());
        }

        JsonReader jsonReader = Json.createReader(new StringReader(jString));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        JsonArray nBestArray = jsonObject.getJsonArray("NBest");

        if (nBestArray == null || nBestArray.isEmpty()) {
            log.warn("인식은 성공했으나 NBest 배열이 비어있습니다.");
            return new SpeechResponseDTO(result.getReason().toString());
        }

        JsonObject nBestItem = nBestArray.getJsonObject(0);
        JsonArray wordsArray = nBestItem.getJsonArray("Words");

        if (wordsArray == null || wordsArray.isEmpty()) {
            log.warn("인식은 성공했으나 Words 배열이 비어있습니다.");
            return new SpeechResponseDTO(result.getReason().toString());
        }

        List<String> recognizedWords = new ArrayList<>();
        List<Word> pronWords = new ArrayList<>();

        for (int j = 0; j < wordsArray.size(); j++) {
            JsonObject wordItem = wordsArray.getJsonObject(j);
            recognizedWords.add(wordItem.getString("Word"));

            JsonObject pronAssessment = wordItem.getJsonObject("PronunciationAssessment");

            String errorType = null;
            double accuracyScore = 0.0;

            if (pronAssessment != null) {
                errorType = pronAssessment.getString("ErrorType", "None");
                javax.json.JsonNumber accuracyScoreJson = pronAssessment.getJsonNumber("AccuracyScore");
                if (accuracyScoreJson != null) {
                    accuracyScore = accuracyScoreJson.doubleValue();
                }
            } else {
                log.warn("PronunciationAssessment 객체가 누락되었습니다.");
                errorType = "No Assessment";
            }
            pronWords.add(new Word(wordItem.getString("Word"), errorType, accuracyScore));
        }

        // Fluency score는 문장 단위로 가져옴
        PronunciationAssessmentResult pronResult = PronunciationAssessmentResult.fromResult(result);
        double fluencyScore = pronResult.getFluencyScore();

        // Omission, Insertion 등 모든 오류를 포함한 finalWords를 직접 구성
        List<Word> finalWords = new ArrayList<>();
        // pronWords에 Insertion/Omission 정보가 포함되어 있음
        finalWords.addAll(pronWords);


        // 최종 점수 계산 로직
        SpeechResponseDTO finalResult = calculateFinalScores(referenceText, finalWords, fluencyScore);

        config.close();
        audioInput.close();
        recognizer.close();

        return finalResult;
    }

    //최종 점수 계산 메서드 (finalWords를 직접 받음)
    private SpeechResponseDTO calculateFinalScores(String referenceText, List<Word> finalWords, double fluencyScore) {
        String[] referenceWords = referenceText.toLowerCase().split(" ");

        double totalAccuracyScore = 0;
        int accuracyCount = 0;
        int validCount = 0;
        for (Word word : finalWords) {
            if (!"Insertion".equals(word.getErrorType())) {
                totalAccuracyScore += word.getAccuracyScore();
                accuracyCount += 1;
            }
            if ("None".equals(word.getErrorType())) {
                validCount += 1;
            }
        }
        double accuracyScore = accuracyCount > 0 ? totalAccuracyScore / accuracyCount : 0;
        double completenessScore = (double) validCount / referenceWords.length * 100;
        completenessScore = completenessScore <= 100 ? completenessScore : 100;

        SpeechResponseDTO resultDto = new SpeechResponseDTO();
        resultDto.setAccuracyScore((float) accuracyScore);
        resultDto.setCompletenessScore((float) completenessScore);
        resultDto.setFluencyScore((float) fluencyScore);

        // List<Word>를 그대로 DTO에 설정
        resultDto.setWords(finalWords);

        return resultDto;
    }
}

