package com.hanzo.transcribeserver.service;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.gson.Gson;
import com.hanzo.transcribeserver.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
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
                    "추가로 문장 중에 퀴즈에 넣을만한 정도의 표현과 길이를 가진 문장은 별도로 quiz_sentences: [{japanese: 일본어, korean: 한국어, level: 난이도}, ...] 형식으로 주세요.";
                    /*"퀴즈의 일본어 텍스트는 조사의 뒤나 하나의 단어 뒤에서 구분해서 배열 형태로 나누어 주세요.";*/

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
                        GeminiResponseDTO geminiDto = gson.fromJson(rawResponse, GeminiResponseDTO.class);
                        tokenizeJapanese(geminiDto);
                        log.debug("[final result]: {}", geminiDto);
                        return geminiDto;
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

    // kuromoji 토큰화
    public GeminiResponseDTO tokenizeJapanese(GeminiResponseDTO geminiDto) throws JsonProcessingException {
        GeminiResponseDTO response = geminiDto;
        Tokenizer tokenizer = new Tokenizer();

        List<SentenceDTO> sentences = response.getSentences();
        List<QuizDTO> quizzes = response.getQuizSentences();

        for(SentenceDTO sentence : sentences) {
            List<TokenDTO> tokenDtoList = new ArrayList<>();
            List<Token> tokens = tokenizer.tokenize(sentence.getJapanese());

            for(Token token : tokens) {
                TokenDTO tokenDto = TokenDTO.builder()
                        .index(token.getPosition())
                        .surface(token.getSurface())
                        .base(token.getBaseForm())
                        .pos(token.getPartOfSpeechLevel1())
                        .reading(token.getReading())
                        .build();

                tokenDtoList.add(tokenDto);
            }
            sentence.setJapaneseTokens(tokenDtoList);
        }

        geminiDto.setSentences(sentences);

        for(QuizDTO quiz : quizzes) {
            // 토큰화
            List<Token> tokens = tokenizer.tokenize(quiz.getJapanese());

            // 퀴즈용 토큰 묶기
            List<String> chunks = createChunks(tokens);

            ObjectMapper mapper = new ObjectMapper();
            quiz.setJapanese(mapper.writeValueAsString(chunks));


        }

        return response;
    }

    // 형태소 결합 메소드
    public static List<String> createChunks(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            StringBuilder currentChunk = new StringBuilder(tokens.get(i).getSurface());

            // 현재 청크에 다음 토큰을 계속해서 결합할 수 있는지 확인
            while (i + 1 < tokens.size()) {
                Token lastTokenInChunk = tokens.get(i);
                Token nextToken = tokens.get(i + 1);

                if (canCombine(lastTokenInChunk, nextToken)) {
                    currentChunk.append(nextToken.getSurface());
                    i++; // 다음 토큰을 사용했으므로 인덱스 증가
                } else {
                    break; // 더 이상 결합할 수 없으면 중단
                }
            }
            chunks.add(currentChunk.toString());
        }
        return chunks;
    }

    /**
     * 두 토큰을 하나의 덩어리로 결합할 수 있는지 판단하는 규칙 메소드
     */
    private static boolean canCombine(Token current, Token next) {
        // 규칙 1: 접두사 + 명사 (예: お + ばあちゃん)
        if (isPrefix(current) && isNoun(next)) {
            return true;
        }

        // 규칙 2: 명사 + 조사 (예: 心 + は)
        if (isNoun(current) && isParticle(next)) {
            return true;
        }

        // 규칙 3: 동사/형용사 + 활용 어미 (조동사, 보조동사, 접속조사 등)
        // (예: 閉ざさ + れた, 開い + て, 明るく + ない)
        if ((isVerb(current) || isAdjective(current)) && isContinuation(next)) {
            return true;
        }

        // 규칙 4: 보조 동사 결합 (-て いく/くる)
        // (예: 開いて + いった)
        if (isConjunctiveParticle(current) && isVerb(next) && isAuxiliaryCompoundVerb(next)) {
            return true;
        }

        // 규칙 5: 연속된 어미/조사 결합 (예: んだ + よ, ました + ね)
        if (isEnding(current) && isEnding(next)) {
            return true;
        }

        return false;
    }

    // --- 품사 판별을 위한 헬퍼 메소드들 ---
    private static boolean isNoun(Token token) { return token.getPartOfSpeechLevel1().equals("名詞"); }
    private static boolean isVerb(Token token) { return token.getPartOfSpeechLevel1().equals("動詞"); }
    private static boolean isAdjective(Token token) { return token.getPartOfSpeechLevel1().equals("形容詞"); }
    private static boolean isParticle(Token token) { return token.getPartOfSpeechLevel1().equals("助詞"); }
    private static boolean isAuxiliaryVerb(Token token) { return token.getPartOfSpeechLevel1().equals("助動詞"); }
    private static boolean isPrefix(Token token) { return token.getPartOfSpeechLevel1().equals("接頭詞"); }
    private static boolean isSymbol(Token token) { return token.getPartOfSpeechLevel1().equals("記号"); }

    private static boolean isConjunctiveParticle(Token token) {
        return isParticle(token) && token.getPartOfSpeechLevel2().equals("接続助詞");
    }

    private static boolean isNonIndependentVerb(Token token) {
        return isVerb(token) && token.getPartOfSpeechLevel2().equals("非自立");
    }

    // -ていく, -てくる, -てしまう 등의 보조 동사인지 확인
    private static boolean isAuxiliaryCompoundVerb(Token token) {
        String baseForm = token.getBaseForm();
        return baseForm.equals("行く") || baseForm.equals("来る") || baseForm.equals("しまう");
    }

    // 동사/형용사 뒤에 붙어 활용형을 만드는 요소인지 확인
    private static boolean isContinuation(Token token) {
        return isAuxiliaryVerb(token) || isNonIndependentVerb(token) || isConjunctiveParticle(token);
    }

    // 문장 끝에 오는 어미 요소인지 확인 (결합용)
    private static boolean isEnding(Token token) {
        return isAuxiliaryVerb(token) || isParticle(token) || isSymbol(token) && !token.getSurface().equals("、");
    }
}
