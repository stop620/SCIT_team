package com.hanzo.mochilearn.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.dto.CardSaveDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.repository.CardRepository;
import com.hanzo.mochilearn.repository.SentenceRepository;
import com.hanzo.mochilearn.service.CardService;
import com.hanzo.mochilearn.service.QuizService;
import com.hanzo.mochilearn.service.SentenceService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@RestController
@RequiredArgsConstructor
public class StudyRestController {

    private final CardService cardService;
    private final QuizService quizService;
    private final SentenceService sentenceService;
    private final CardRepository cardRepository;
    private final SentenceRepository sentenceRepository;
    
    // 학습 카드 로드
    @GetMapping("/api/study/load")
    public List<CardDTO> getCards(
            @RequestParam(name="sort", defaultValue = "popular") String sort,
            @RequestParam(name="page", defaultValue = "0") int page,
            @RequestParam(name="size", defaultValue = "12") int size,
            @RequestParam(name ="search", defaultValue = "") String search) {

        if (search == null || search.isEmpty()) {
            return cardService.getPagedCards(sort, page, size);
        }

        // 통합검색: 제목 또는 닉네임(멤버) 포함 검색
        return cardService.searchCardsByTitleOrNickname(sort, page, size, search);
    }
    //태그필터링
    @PostMapping("/api/study/filterByTags")
    public Page<CardDTO> filterCardsByTags(
            @RequestBody List<String> tags,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "popular") String sort) {

        return cardService.searchCardsByTags(tags, page, size, sort);
    }


    @GetMapping("/api/study/card")
    public CardDTO cardRead(@RequestParam("cardId") Integer cardId) {
    	CardEntity cardEntity = cardService.findCardById(cardId);
    	CardDTO cardDTO = cardService.toSimpleDTO(cardEntity);
        log.debug("CardDTO: {}", cardDTO);
        return cardDTO;
    	
    }
    
    // 프론트엔드에서 보낸 학습 카드 데이터를 받아 DB에 저장하는 API
    @PostMapping("/api/study/save")
    @Transactional
    public ResponseEntity<?> saveLearningCard(@RequestBody CardSaveDTO cardSaveDto) {
        log.info("저장할 학습 카드 데이터 : {}", cardSaveDto.getCardDTO());

        try {
            // Card 엔티티 생성 및 저장
            // TODO: 유저정보 생기면 로직 추가해야됨 지금은 1로 임의 저장
            Integer cardId = cardService.save(cardSaveDto.getCardDTO(), 1);
            quizService.save(cardSaveDto.getQuizDtoList(), cardId);

            return ResponseEntity.ok(Map.of("message", "카드 데이터 저장 성공", "cardId", cardId));

        } catch (Exception e) {
            log.error("카드 데이터 저장 오류", e);
            // TODO: 오류 메시지 세분화 필요
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "저장 오류"));
        }
    }
    @DeleteMapping("/api/study/card/{cardId}")
    public String deleteCard(@PathVariable("cardId") Integer cardId) {
        boolean deleted = cardService.deleteCardById(cardId);
        if (deleted) {
            return "삭제 성공";
        } else {
            return "삭제 실패: 해당 카드 없음";
        }
    }
    

    @PostMapping("/api/study/likes/toggle")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @RequestParam("memberId") Integer memberId,
            @RequestParam("cardId") Integer cardId) {
        String state = cardService.toggleLike(memberId, cardId);
        Map<String, Object> result = new HashMap<>();
        result.put("state", state);
        return ResponseEntity.ok(result);
    }
    
}
