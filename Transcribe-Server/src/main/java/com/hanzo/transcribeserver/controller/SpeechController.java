package com.hanzo.transcribeserver.controller;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import java.util.concurrent.Semaphore;


@Slf4j
@RequiredArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:9000")
public class SpeechController {

    // application.properties에서 키 목록을 List<String>으로 주입
    @Value("#{'${azure.api.keys}'.split(',')}")
    private List<String> azureKeys;

    @Value("${azure.api.url}")
    private String azureApiUrl;

    private static final String speechRegion = "koreacentral";

    // URL과 시간 정보를 받아 자막 추출 결과를 반환
    private static Semaphore stopRecognitionSemaphore;

    // Word 객체는 샘플 코드에서와 동일하게 별도로 정의 필요
    static class Word {
        String word;
        String errorType;
        double accuracyScore;

        Word(String word, String errorType) {
            this.word = word;
            this.errorType = errorType;
        }

        Word(String word, String errorType, double accuracyScore) {
            this.word = word;
            this.errorType = errorType;
            this.accuracyScore = accuracyScore;
        }
    }

    @PostMapping("/api/speech")
    public Map<String, Object> assessPronunciation(@RequestParam("audioFile") MultipartFile file,
                                                   @RequestParam("referenceText") String referenceText) throws Exception {
        log.debug("요청수신");
        File webmFile = File.createTempFile("audio-", ".webm");
        File wavFile = File.createTempFile("audio-", ".wav");
        log.debug("임시파일생성");

        try (FileOutputStream fos = new FileOutputStream(webmFile)) {
            fos.write(file.getBytes());
        }

        // FFmpeg 명령어를 설정합니다.
        ProcessBuilder ffmpegProcessBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", webmFile.getAbsolutePath(), // 입력 파일
                "-ar", "16000",                 // 샘플링 레이트 16kHz
                "-ac", "1",                     // 오디오 채널 1 (모노)
                wavFile.getAbsolutePath()       // 출력 파일
        );
        log.debug("ffmpeg설정");

        // FFmpeg 프로세스를 실행하고 종료될 때까지 기다립니다.
        Process ffmpegProcess = ffmpegProcessBuilder.start();
        int exitCode = ffmpegProcess.waitFor();
        

        if (exitCode != 0) {
            // FFmpeg 변환 실패 시 에러 로그를 읽고 예외를 발생시킵니다.
            try (InputStream errorStream = ffmpegProcess.getErrorStream()) {
                String error = new String(errorStream.readAllBytes());
                throw new RuntimeException("FFmpeg 변환 실패. 에러 코드: " + exitCode + ", 에러 메시지: " + error);
            }
        }
        log.debug("ffmpeg완료");
        
        SpeechConfig config = SpeechConfig.fromSubscription(azureKeys.get(0), speechRegion);
        config.setSpeechRecognitionLanguage("ja-JP");

        AudioConfig audioInput = AudioConfig.fromWavFileInput(wavFile.getAbsolutePath());
        log.debug("기본세팅");

        stopRecognitionSemaphore = new Semaphore(0);
        List<String> recognizedWords = new ArrayList<>();
        List<Word> pronWords = new ArrayList<>();
        List<Double> fluencyScores = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        SpeechRecognizer recognizer = new SpeechRecognizer(config, audioInput);
        log.debug("ai 요청함");

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String jString = e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
                JsonReader jsonReader = Json.createReader(new StringReader(jString));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();

                JsonArray nBestArray = jsonObject.getJsonArray("NBest");

                if (nBestArray != null && !nBestArray.isEmpty()) {
                    JsonObject nBestItem = nBestArray.getJsonObject(0); // 첫 번째 NBest 결과만 사용
                    JsonArray wordsArray = nBestItem.getJsonArray("Words");
                    long durationSum = 0;

                    for (int j = 0; j < wordsArray.size(); j++) {
                        JsonObject wordItem = wordsArray.getJsonObject(j);
                        recognizedWords.add(wordItem.getString("Word"));
                        durationSum += wordItem.getJsonNumber("Duration").longValue();

                        JsonObject pronAssessment = wordItem.getJsonObject("PronunciationAssessment");
                        pronWords.add(new Word(wordItem.getString("Word"), pronAssessment.getString("ErrorType"), pronAssessment.getJsonNumber("AccuracyScore").doubleValue()));
                    }
                    durations.add(durationSum);
                }

                // Sentence-level scores
                PronunciationAssessmentResult pronResult = PronunciationAssessmentResult.fromResult(e.getResult());
                fluencyScores.add(pronResult.getFluencyScore());
            }
        });

        recognizer.canceled.addEventListener((s, e) -> stopRecognitionSemaphore.release());
        recognizer.sessionStopped.addEventListener((s, e) -> stopRecognitionSemaphore.release());

        // Pronunciation Assessment Config 설정
        PronunciationAssessmentConfig pronunciationConfig = new PronunciationAssessmentConfig(referenceText,
                PronunciationAssessmentGradingSystem.HundredMark, PronunciationAssessmentGranularity.Word, true);
        pronunciationConfig.applyTo(recognizer);

        // 연속 인식 시작
        recognizer.recognizeOnceAsync().get();
        recognizer.stopContinuousRecognitionAsync().get();
        // 샘플 코드처럼 연속으로 여러 문장 처리 시 startContinuousRecognitionAsync(), 단일 파일은 recognizeOnceAsync() 사용

        Map<String, Object> finalResult = calculateFinalScores(referenceText, recognizedWords, pronWords, fluencyScores, durations);
        log.debug("작업끝");
        
        // 자원 해제
        recognizer.close();
        audioInput.close();
        // 임시 파일 삭제
        webmFile.delete();
        wavFile.delete();
        log.debug(finalResult.toString());
        return finalResult;
    }

    private Map<String, Object> calculateFinalScores(String referenceText, List<String> recognizedWords, List<Word> pronWords, List<Double> fluencyScores, List<Long> durations) {
        // 샘플 코드의 최종 계산 로직을 그대로 사용
        List<Word> finalWords = new ArrayList<>();
        String[] referenceWords = referenceText.toLowerCase().split(" ");
        for (int j = 0; j < referenceWords.length; j++) {
            referenceWords[j] = referenceWords[j].replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
        }

        Patch<String> diff = DiffUtils.diff(Arrays.asList(referenceWords), recognizedWords, true);

        int currentIdx = 0;
        for (AbstractDelta<String> d : diff.getDeltas()) {
            if (d.getType() == DeltaType.EQUAL) {
                for (int i = currentIdx; i < currentIdx + d.getSource().size(); i++) {
                    finalWords.add(pronWords.get(i));
                }
                currentIdx += d.getTarget().size();
            }
            if (d.getType() == DeltaType.DELETE || d.getType() == DeltaType.CHANGE) {
                for (String w : d.getSource().getLines()) {
                    finalWords.add(new Word(w, "Omission"));
                }
            }
            if (d.getType() == DeltaType.INSERT || d.getType() == DeltaType.CHANGE) {
                for (int i = currentIdx; i < currentIdx + d.getTarget().size(); i++) {
                    Word w = pronWords.get(i);
                    w.errorType = "Insertion";
                    finalWords.add(w);
                }
                currentIdx += d.getTarget().size();
            }
        }

        double totalAccuracyScore = 0;
        int accuracyCount = 0;
        int validCount = 0;
        for (Word word : finalWords) {
            if (!"Insertion".equals(word.errorType)) {
                totalAccuracyScore += word.accuracyScore;
                accuracyCount += 1;
            }
            if ("None".equals(word.errorType)) {
                validCount += 1;
            }
        }
        double accuracyScore = accuracyCount > 0 ? totalAccuracyScore / accuracyCount : 0;
        double completenessScore = (double) validCount / referenceWords.length * 100;
        completenessScore = completenessScore <= 100 ? completenessScore : 100;

        // Fluency score 계산 (여기서는 단일 문장이라 `recognizeOnceAsync`의 결과를 사용해도 무방)
        double fluencyScoreSum = 0;
        long durationSum = 0;
        for (int i = 0; i < durations.size(); i++) {
            fluencyScoreSum += fluencyScores.get(i) * durations.get(i);
            durationSum += durations.get(i);
        }
        double fluencyScore = durationSum > 0 ? fluencyScoreSum / durationSum : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("accuracyScore", accuracyScore);
        result.put("completenessScore", completenessScore);
        result.put("fluencyScore", fluencyScore);
        result.put("words", finalWords.stream().map(w -> {
            Map<String, Object> wordMap = new HashMap<>();
            wordMap.put("word", w.word);
            wordMap.put("errorType", w.errorType);
            wordMap.put("accuracyScore", w.accuracyScore);
            return wordMap;
        }).toArray());

        return result;
    }
}
