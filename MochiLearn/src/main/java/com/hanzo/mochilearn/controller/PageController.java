package com.hanzo.mochilearn.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.SectionEntity;
import com.hanzo.mochilearn.entity.SentenceEntity;
import com.hanzo.mochilearn.service.CardService;
import com.hanzo.mochilearn.service.SectionService;
import com.hanzo.mochilearn.service.SentenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("page")
public class PageController {

    private final CardService cardService;
    private final SectionService sectionService;

    

    @GetMapping("study")
    public String studyPage(Model model) {
        List<CardDTO> cards = new ArrayList<>();
        model.addAttribute("cards", cards);
        return "page/studyPage";
    }

    @GetMapping("studyCard")
    public String studyCard(@RequestParam("cardId") Integer cardId, Model model) {
        CardEntity entity = cardService.getCardById(cardId);
        CardDTO dto = cardService.toDTO(entity);
        log.debug("{}",dto);
        // 카드와 연관된 섹션 목록 조회
        model.addAttribute("card", dto);
        
        

        return "page/studyCard";
    }

    @GetMapping("quiz")
    public String quizPage() {
        return "page/quizPage";
    }

    @GetMapping("word")
    public String wordPage() {
        return "page/wordPage";
    }

    @GetMapping("mypage")
    public String myPage() {
        return "page/myPage";
    }

    @GetMapping("write")
    public String writePage() {
        return "page/writePage";
    }
}
