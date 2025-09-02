package com.hanzo.transcribeserver.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class SpeechService {

    @Value("${azure.api.key}")
    private String azureKey;

    @Value("${azure.api.url}")
    private String azureApiUrl;

    private static final String speechRegion = "koreacentral";
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

    public void pronunciationAssessment(MultipartFile file, String referenceText) throws ExecutionException, InterruptedException {

        // api Key, service region 설정
        SpeechConfig config = SpeechConfig.fromSubscription(azureKey, speechRegion);
        String lang = "ja-JP";      // 일본어 설정

        //ffmpeg로 wav 파일로 변환
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("speech-audio-");
            log.info("임시 디렉토리 생성: {}", tempDir);

        } catch (Exception e) {}
        File webmFile = tempDir.resolve("audio.webm").toFile();
        File wavFile = tempDir.resolve("audio.wav").toFile();
        log.debug("임시파일생성");

        try (FileOutputStream fos = new FileOutputStream(webmFile)) {
            fos.write(file.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
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
        Process ffmpegProcess = null;
        try {
            ffmpegProcess = ffmpegProcessBuilder.start();
            int exitCode = ffmpegProcess.waitFor();
            log.debug("ffmpeg 종료코드: {}", exitCode);
            if(exitCode != 0) {
                // FFmpeg 변환 실패 시 에러 로그를 읽고 예외를 발생시킵니다.
                try (InputStream errorStream = ffmpegProcess.getErrorStream()) {
                    String error = new String(errorStream.readAllBytes());
                    throw new RuntimeException("FFmpeg 변환 실패. 에러 코드: " + exitCode + ", 에러 메시지: " + error);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(ffmpegProcess != null) {
                ffmpegProcess.destroyForcibly();
            }
        }

        log.debug("ffmpeg완료");

        // speech recognizer 생성
        AudioConfig audioInput = AudioConfig.fromWavFileInput(wavFile.getAbsolutePath());

        stopRecognitionSemaphore = new Semaphore(0);
        List<String> recognizedWords = new ArrayList<>();
        List<Word> pronWords = new ArrayList<>();
        List<Word> finalWords = new ArrayList<>();
        List<Double> fluencyScores = new ArrayList<>();
        List<Double> prosodyScores = new ArrayList<>();
        List<Long> durations = new ArrayList<>();

        SpeechRecognizer recognizer = new SpeechRecognizer(config, lang, audioInput);
        {
            // subscribes to events
            recognizer.recognized.addEventListener((s, e) -> {
                if(e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    System.out.println("[RECOGNIZED] Text=" + e.getResult().getText());
                    PronunciationAssessmentResult pronResult = PronunciationAssessmentResult.fromResult(e.getResult());
                    System.out.println(
                            String.format(
                                    "    Accuracy score: %f, Prosody score: %f, Pronunciation score: %f, Completeness score : %f, FluencyScore: %f",
                                    pronResult.getAccuracyScore(), pronResult.getProsodyScore(), pronResult.getPronunciationScore(),
                                    pronResult.getCompletenessScore(), pronResult.getFluencyScore()));
                    fluencyScores.add(pronResult.getFluencyScore());
                    prosodyScores.add(pronResult.getProsodyScore());

                    String jString = e.getResult().getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
                    JsonReader jsonReader = Json.createReader(new StringReader(jString));
                    JsonObject jsonObject = jsonReader.readObject();
                    jsonReader.close();

                    JsonArray nBestArray = jsonObject.getJsonArray("NBest");

                    for (int i = 0; i < nBestArray.size(); i++) {
                        JsonObject nBestItem = nBestArray.getJsonObject(i);

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
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    System.out.println("NOMATCH: Speech could not be recognized.");
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                System.out.println("CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                    System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                    System.out.println("CANCELED: Did you update the subscription info?");
                }

                stopRecognitionSemaphore.release();
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                System.out.println("\n    Session started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                System.out.println("\n    Session stopped event.");
            });

            boolean enableMiscue = true;
            // The reference matches the input wave named YourAudioFile.wav.
            // String referenceText = "写真や文書数表などオフラインのものもあればインターネット銀行やネットショッピングソーシャルネットワークサービスなどオンラインサービスのアカウントもあるわ";

            // Create pronunciation assessment config, set grading system, granularity and if enable miscue based on your requirement.
            PronunciationAssessmentConfig pronunciationConfig = new PronunciationAssessmentConfig(referenceText,
                    PronunciationAssessmentGradingSystem.HundredMark, PronunciationAssessmentGranularity.Phoneme, enableMiscue);

            pronunciationConfig.enableProsodyAssessment();

            pronunciationConfig.applyTo(recognizer);

            // Starts continuous recognition. Uses stopContinuousRecognitionAsync() to stop recognition.
            recognizer.startContinuousRecognitionAsync().get();

            // Waits for completion.
            stopRecognitionSemaphore.acquire();

            recognizer.stopContinuousRecognitionAsync().get();

            // For continuous pronunciation assessment mode, the service won't return the words with `Insertion` or `Omission`
            // even if miscue is enabled.
            // We need to compare with the reference text after received all recognized words to get these error words.
            String[] referenceWords = referenceText.toLowerCase().split(" ");
            for (int j = 0; j < referenceWords.length; j++) {
                referenceWords[j] = referenceWords[j].replaceAll("^\\p{Punct}+|\\p{Punct}+$","");
            }

            if (enableMiscue) {
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
            }
            else {
                finalWords = pronWords;
            }

            //We can calculate whole accuracy by averaging
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
            double accuracyScore = totalAccuracyScore / accuracyCount;

            //Re-calculate fluency score
            double fluencyScoreSum = 0;
            long durationSum = 0;
            for (int i = 0; i < durations.size(); i++) {
                fluencyScoreSum += fluencyScores.get(i)*durations.get(i);
                durationSum += durations.get(i);
            }
            double fluencyScore = fluencyScoreSum / durationSum;

            //Re-calculate prosody score
            double prosodyScoreSum = 0;
            for (int i = 0; i < prosodyScores.size(); i++) {
                prosodyScoreSum += prosodyScores.get(i);
            }
            double prosodyScore = prosodyScoreSum / prosodyScores.size();

            // Calculate whole completeness score
            double completenessScore = (double)validCount / referenceWords.length * 100;
            completenessScore = completenessScore <= 100 ? completenessScore : 100;

            System.out.println("Paragraph accuracy score: " + accuracyScore + " prosody score: " + prosodyScore +
                    ", completeness score: " +completenessScore +
                    " , fluency score: " + fluencyScore);
            for (Word w : finalWords) {
                System.out.println(" word: " + w.word + "\taccuracy score: " +
                        w.accuracyScore + "\terror type: " + w.errorType);
            }
        }
        config.close();
        audioInput.close();
        recognizer.close();
    }
}
