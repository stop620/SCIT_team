package com.hanzo.mochilearn.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.dto.SectionDTO;
import com.hanzo.mochilearn.dto.SentenceDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.SectionEntity;
import com.hanzo.mochilearn.entity.SentenceEntity;
import com.hanzo.mochilearn.repository.CardRepository;
import com.hanzo.mochilearn.repository.SectionRepository;
import com.hanzo.mochilearn.repository.SentenceRepository;
import com.hanzo.mochilearn.service.CardService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
@RestController
public class StudyRestController {

    // --- 의존성 주입 ---
    // 실제로는 이 로직을 처리하는 Service 계층을 주입하는 것이 좋습니다.

    private final CardService cardService;




    /**
     * 프론트엔드에서 보낸 학습 카드 데이터를 받아 DB에 저장하는 API
     */
    @PostMapping("/api/study/save")
    @Transactional // 여러 테이블에 걸친 작업을 하나의 트랜잭션으로 묶어 데이터 정합성 보장
    public ResponseEntity<?> saveLearningCard(@RequestBody CardDTO cardDto) {
        log.info("Received Learning Card Data for saving: {}", cardDto);

        try {
            // TODO: 유저정보 생기면 로직 추가

            // 1. Card 엔티티 생성 및 저장
            cardService.save(cardDto, 1);

            return ResponseEntity.ok(Map.of("message", "Data saved successfully", "cardId", cardDto.getId()));

        } catch (Exception e) {
            log.error("Error saving learning card", e);
            // TODO: 좀 더 구체적인 에러 메시지를 반환하는 것이 좋습니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error saving data"));
        }
    }
}
