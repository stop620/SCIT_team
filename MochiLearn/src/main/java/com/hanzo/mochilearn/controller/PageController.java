package com.hanzo.mochilearn.controller;

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

    

    @GetMapping("study")
    public String studyPage() {
//        List<CardDTO> cards = new ArrayList<>();
//        model.addAttribute("cards", cards);
        return "page/studyPage";
    }

    @GetMapping("studyCard")
    public String studyCard(@RequestParam("cardId") Integer cardId, Model model) {
        model.addAttribute("cardId", cardId);
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
