package com.hanzo.mochilearn.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.service.CardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("page")
public class PageController {
	private final CardService cardService;

    @GetMapping("study")
    public String studyPage(Model model) {
        List<CardDTO> cards = cardService.getAllCards();
        model.addAttribute("cards", cards);
        return "page/studyPage";  
    }
    @GetMapping("studyCard")
    public String studyCard(@RequestParam("cardId") Integer cardId, Model model) {
        CardEntity entity = cardService.getCardById(cardId);
        CardDTO dto= cardService.toDTO(entity);
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
}
