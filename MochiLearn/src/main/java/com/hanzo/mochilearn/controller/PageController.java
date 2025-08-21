package com.hanzo.mochilearn.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hanzo.mochilearn.dto.StudyDTO;
import com.hanzo.mochilearn.entity.StudyEntity;
import com.hanzo.mochilearn.service.StudyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("page")
public class PageController {
	private final StudyService ss;

    @GetMapping("study")
    public String studyPage(Model model) {
        List<StudyDTO> cards = ss.getAllCards();
        model.addAttribute("cards", cards);
        return "page/studyPage";  
    }
    @GetMapping("studyCard")
    public String studyCard(@RequestParam("cardId") Long cardId, Model model) {
        StudyEntity entity = ss.getCardById(cardId);
        StudyDTO dto= ss.toDTO(entity);
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
