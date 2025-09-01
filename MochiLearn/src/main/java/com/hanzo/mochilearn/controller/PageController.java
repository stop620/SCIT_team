package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.service.CardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("page")
public class PageController {

    private final CardService cardService;

    public PageController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("study")
    public String studyPage(Model model) {
        List<CardDTO> cards = new ArrayList<>();
        model.addAttribute("cards", cards);
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

    @GetMapping("/best")
    public String bestPage() {
        return "page/bestPage";
    }

	@GetMapping({"wordCard2","wordCard1"})
	public String wordCard() {
		return "page/wordCardPage";
	}
	
	@GetMapping("addWordCard")
	public String addWord() {
		return "page/addWordCardPage";
	}
	
	@PostMapping("addWordCard")
	public String addWordCard() {
		return "page/wordPage";
	}
	
	@GetMapping("quizCardPage")
	public String quizCardPage() {

		return "page/quizCardPage";
	}

}
