package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.QuizDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.MemberQuizHistoryEntity;
import com.hanzo.mochilearn.entity.QuizEntity;
import com.hanzo.mochilearn.repository.CardRepository;
import com.hanzo.mochilearn.repository.MemberQuizHistoryRepository;
import com.hanzo.mochilearn.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final CardRepository cardRepository;
    private final MemberQuizHistoryRepository memberQuizHistoryRepository;
    private final int quizCount = 5;

    public void save(List<QuizDTO> quizDtoList, Integer cardId){

        if(quizDtoList.size() > 0) {
            for (QuizDTO quizDto : quizDtoList) {
                String jpString = String.join(",", quizDto.getJapanese());
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

    public List<QuizDTO> makeQuiz(int level, int memberId) {

        // 사용자가 이전에 수행한 퀴즈 조회
        List<MemberQuizHistoryEntity> userHistory = memberQuizHistoryRepository.findByMemberId(memberId);
        // 퀴즈 id 리스트로 변환
        List<Integer> solvedQuizIds = userHistory.stream()
                .map(history -> history.getQuiz().getId())
                .collect(Collectors.toList());

        // 퀴즈 DB에서 조회 (level, 풀지 않은 퀴즈, 무작위 5개)
        List<QuizEntity> quizEntities = new ArrayList<>();

        if(solvedQuizIds.isEmpty()) { // 푼적 없는 경우는 그냥 난이도만 체크
            quizEntities = quizRepository.findRandomQuizByLevel(level, quizCount);
        } else { // 풀지 않은 퀴즈 조회
            quizEntities = quizRepository.findUnsolvedRandomQuizByLevel(level, quizCount, solvedQuizIds);
        }
        // 조회한 퀴즈 엔티티를 보낼 DTO로 변환
        return quizEntities.stream()
                .map(quizEntity -> {
                    // 일본어 문장(쉼표로 구분된 문자열)을 단어 리스트로 변환합니다.
                    List<String> japaneseWords = Arrays.asList(quizEntity.getJapanese().split(","));

                    // 단어 리스트의 순서를 무작위로 섞습니다.
                    Collections.shuffle(japaneseWords);

                    // 최종 DTO를 생성하여 반환합니다.
                    return new QuizDTO(
                            quizEntity.getId(),
                            japaneseWords,
                            quizEntity.getKorean(),
                            level
                    );
                })
                .collect(Collectors.toList());
    }
}
