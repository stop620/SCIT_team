package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.QuizDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.QuizEntity;
import com.hanzo.mochilearn.repository.CardRepository;
import com.hanzo.mochilearn.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final CardRepository cardRepository;

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
}
