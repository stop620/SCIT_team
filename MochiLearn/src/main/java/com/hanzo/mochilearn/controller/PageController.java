package com.hanzo.mochilearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("page")
public class PageController {

    @GetMapping("study")
    public String studyPage() {
        return "page/studyPage";
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
