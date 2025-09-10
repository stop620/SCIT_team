package com.hanzo.mochilearn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanzo.mochilearn.dto.quiz.QuizDTO;
import com.hanzo.mochilearn.dto.quiz.QuizResponseDTO;
import com.hanzo.mochilearn.dto.quiz.QuizResultDTO;
import com.hanzo.mochilearn.dto.quiz.QuizType;
import com.hanzo.mochilearn.entity.*;
import com.hanzo.mochilearn.entity.card.CardEntity;
import com.hanzo.mochilearn.entity.quiz.QuizAnswerEntity;
import com.hanzo.mochilearn.entity.quiz.QuizAttemptEntity;
import com.hanzo.mochilearn.entity.quiz.QuizEntity;
import com.hanzo.mochilearn.entity.quiz.QuizSessionEntity;
import com.hanzo.mochilearn.repository.*;
import com.hanzo.mochilearn.repository.card.CardRepository;
import com.hanzo.mochilearn.repository.quiz.QuizAnswerRepository;
import com.hanzo.mochilearn.repository.quiz.QuizAttemptRepository;
import com.hanzo.mochilearn.repository.quiz.QuizRepository;
import com.hanzo.mochilearn.repository.quiz.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final CardRepository cardRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final MemberRepository memberRepository;
    private final Random random = new Random();
    private final int quizCount = 5;

    public void save(List<QuizDTO> quizDtoList, Integer cardId){

        if(quizDtoList.size() > 0) {
            for (QuizDTO quizDto : quizDtoList) {
                String jpString = quizDto.getJapanese();//String.join(",", quizDto.getJapanese());
                CardEntity cardEntity = cardRepository.findById(cardId)
                        .orElseThrow(()-> new RuntimeException("cardEntity not found"));
                log.debug("퀴즈의 cardEntity = {}", cardEntity);

                QuizEntity quizEntity = QuizEntity.builder()
                        .japanese(jpString)
                        .korean(quizDto.getKorean())
                        .level(quizDto.getLevel())
                        .card(cardEntity)
                        .build();

                log.debug("cardEntity: {}", quizEntity);

                quizRepository.save(quizEntity);
            }
        }

    }

    public List<QuizResponseDTO> makeQuiz(int level, int memberId) {

        // 랜덤 퀴즈 문장 가져오기
        List<QuizEntity> quizEntityList = getRandomQuiz(level, memberId);

        // 랜덤 문장을 문제 형태로 가공해서 반환
        return quizEntityList.stream()
                .map(this::createRandomQuizDto)
                .collect(Collectors.toList());
    }

    private List<QuizEntity> getRandomQuiz(int level, int memberId) {

        // 사용자가 이전에 수행한 퀴즈 중 맞춘 퀴즈 ID 조회
        // 1. 유저가 했던 퀴즈 세션 id 리스트 조회
        List<Integer> solvedQuizIds = quizSessionRepository.FindAllIdByMemberId(level, memberId);
        log.debug("[solvedQuizIds] = {}", solvedQuizIds);

        // 퀴즈 DB에서 조회 (level, 풀지 않은 퀴즈, 무작위 5개)
        List<QuizEntity> quizEntities = new ArrayList<>();

        if(solvedQuizIds.isEmpty()) { // 푼적 없는 경우는 그냥 난이도만 체크
            quizEntities = quizRepository.findRandomQuizByLevel(level, quizCount);
        } else { // 풀지 않은 퀴즈 조회
            quizEntities = quizRepository.findUnsolvedRandomQuizByLevel(level, quizCount, solvedQuizIds);
        }

        return quizEntities;
    }

    private QuizResponseDTO createRandomQuizDto(QuizEntity quizEntity) {
        QuizType quizType = QuizType.values()[random.nextInt(QuizType.values().length)]; // 랜덤 타입 생성
        List<String> originalWords = Arrays.asList(quizEntity.getJapanese().split(",")); // 원본 퀴즈 단어 배열

        switch (quizType) {
            case SHUFFLE:
                return createScrambleQuiz(quizEntity, originalWords);
            case BLANK:
                return originalWords.size() > 2 ? createBlankQuiz(quizEntity, originalWords) : createScrambleQuiz(quizEntity, originalWords);
            case CHOICE:
                return createChoiceQuiz(quizEntity, originalWords);
            default:
                // 예외의 경우, 순서 퀴즈를 반환
                return createScrambleQuiz(quizEntity, originalWords);
        }
    }

    // 순서 섞기 퀴즈 생성
    private QuizResponseDTO createScrambleQuiz(QuizEntity quizEntity, List<String> originalWords) {

        List<String> shuffledWords = new ArrayList<>(originalWords);
        Collections.shuffle(shuffledWords);

        QuizResponseDTO dto = new QuizResponseDTO();
        dto.setQuizId(quizEntity.getId());
        dto.setQuizType(QuizType.SHUFFLE);
        dto.setKorean(quizEntity.getKorean());
        dto.setShuffledSentence(shuffledWords);
        dto.setShuffleAnswer(originalWords);

        return dto;
    }

    // 빈칸 퀴즈 생성
    private QuizResponseDTO createBlankQuiz(QuizEntity quizEntity, List<String> originalWords) {
        // 문장 길이에 따라 빈칸 수를 결정 (최소 1개, 최대 3개)
        int numBlanks = Math.max(1, Math.min(3, originalWords.size() / 4));

        // 전체 인덱스 리스트를 만들어 섞은 뒤, 앞에서부터 빈칸 수만큼 선택
        List<Integer> indices = IntStream.range(0, originalWords.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(indices);
        List<Integer> blankIndices = indices.subList(0, numBlanks);

        // 정답 단어 목록과 빈칸이 포함된 문장 목록을 생성
        List<String> answerWords = new ArrayList<>();
        List<String> sentenceWithBlanks = new ArrayList<>(originalWords);
        for (int index : blankIndices) {
            answerWords.add(originalWords.get(index));
            sentenceWithBlanks.set(index, "______");
        }

        // 빈칸에 들어갈 보기 생성 (정답 단어 + 오답 단어)
        List<String> choices = new ArrayList<>(answerWords);
        // TODO: 오답 보기를 생성하는 로직이 필요합니다. (예: DB에서 다른 단어 가져오기)
        choices.add(this.getRandomWord());
        choices.add(this.getRandomWord());
        choices.add(this.getRandomWord());
        Collections.shuffle(choices);

        QuizResponseDTO dto = new QuizResponseDTO();
        dto.setQuizId(quizEntity.getId());
        dto.setQuizType(QuizType.BLANK);
        dto.setKorean(quizEntity.getKorean());
        dto.setBlankSentence(sentenceWithBlanks);
        dto.setBlankChoices(choices);
        dto.setBlankAnswer(answerWords);
        return dto;
    }

    // 맞는 문장 고르기 퀴즈
    private QuizResponseDTO createChoiceQuiz(QuizEntity quizEntity, List<String> originalWords) {
        // 정답 문장 생성
        String correctAnswer = String.join("", originalWords);
        List<String> choices = new ArrayList<>();
        choices.add(correctAnswer);

        for(int i = 0; i < 2; ++i) {
            // 오답 문장 생성 (단어 순서를 섞음)
            List<String> shuffledWords = new ArrayList<>(originalWords);
            Collections.shuffle(shuffledWords);
            String wrongAnswer = String.join("", shuffledWords);

            // 섞어도 정답과 같을 경우 다시 섞음
            if (correctAnswer.equals(wrongAnswer) && originalWords.size() > 1) {
                Collections.shuffle(shuffledWords);
                wrongAnswer = String.join("", shuffledWords);
            }
            choices.add(wrongAnswer);

        }

        // 보기 리스트를 만들고 순서를 섞음
        Collections.shuffle(choices);

        QuizResponseDTO dto = new QuizResponseDTO();
        dto.setQuizId(quizEntity.getId());
        dto.setQuizType(QuizType.CHOICE);
        dto.setKorean(quizEntity.getKorean());
        dto.setChoiceSentences(choices);
        dto.setChoiceAnswer(correctAnswer);
        return dto;
    }

    // 빈칸 맞추기 퀴즈를 위한 랜덤 오답 단어 찾는 메소드
    private String getRandomWord() {
        String randomSentence = quizRepository.findRandomQuiz().orElseThrow(() -> new RuntimeException("random quiz not found"));
        List<String> words = Arrays.asList(randomSentence.split(","));

        return words.get(random.nextInt(words.size()));
    }

    // 퀴즈 결과 저장 메소드
    public Integer saveResult(List<QuizResultDTO> quizResultDtoList, int memberId) {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("member not found"));

        // 출제했던 퀴즈 dto 리스트
        List<QuizResponseDTO> quizData = quizResultDtoList.stream().map(dto -> dto.getQuiz()).collect(Collectors.toList());
        ObjectMapper mapper = new ObjectMapper();

        try {
            String quizJson = mapper.writeValueAsString(quizData);

            log.debug(quizJson);

            QuizSessionEntity quizSessionEntity = QuizSessionEntity.builder()
                    .member(member)
                    .level(quizResultDtoList.get(0).getLevel())
                    .quizData(quizJson)
                    .build();

            QuizSessionEntity savedSessionEntity = quizSessionRepository.save(quizSessionEntity);

            log.debug("[Quiz Result Save] Save Quiz Session: ID = {}, Entity = {}", savedSessionEntity.getId(), savedSessionEntity);

            Long score = quizResultDtoList.stream().filter(QuizResultDTO::isCorrect).count();

            QuizAttemptEntity quizAttemptEntity = QuizAttemptEntity.builder()
                    .session(savedSessionEntity)
                    //TODO: 시도 횟수 구분 로직 추가해야함 1: 첫시도, 2: 재시도
                    .attemptNo(1)
                    .score(score.intValue())
                    .build();

            QuizAttemptEntity savedAttemptEntity = quizAttemptRepository.save(quizAttemptEntity);

            log.debug("[Quiz Result Save] save Quiz Attempt: ID = {}, Entity = {}", savedAttemptEntity.getAttemptId(), savedAttemptEntity);

            int index = 0;
            for(QuizResultDTO quizResultDTO : quizResultDtoList) {
                index++;
                QuizAnswerEntity quizAnswerEntity = QuizAnswerEntity.builder()
                        .attempt(savedAttemptEntity)
                        .quizId(quizResultDTO.getQuizId())
                        .questionNo(index)
                        .userAnswer(quizResultDTO.getUserAnswer().toString())
                        .isCorrect(quizResultDTO.isCorrect())
                        .build();

                Integer savedAnswerId = quizAnswerRepository.save(quizAnswerEntity).getAnswerId();
                log.debug("[Quiz Result Save] save Quiz Answer: ID = {}", savedAnswerId);
            }

        } catch (JsonProcessingException e) {
            new RuntimeException(e.getMessage());
        }

        return memberId;
    }
}
