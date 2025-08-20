package com.hanzo.mochilearn.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hanzo.mochilearn.dto.StudyDTO;
import com.hanzo.mochilearn.service.StudyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j

public class StudyController {
	private final StudyService ss;

    @GetMapping("page/study")
    public String studyPage(Model model) {
        List<StudyDTO> cards = ss.getAllCards();
        model.addAttribute("cards", cards);
        return "page/studyPage";  
    }
}
