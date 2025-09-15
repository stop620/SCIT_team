package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.word.BookDTO;
import com.hanzo.mochilearn.dto.word.WordDTO;
import com.hanzo.mochilearn.service.BookService;
import com.hanzo.mochilearn.service.CardService;
import com.hanzo.mochilearn.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("page")
public class PageController {

    private final CardService cardService;
    private final WordService wordService;
    private final BookService bookService;

    @GetMapping("study")
    public String studyPage() {
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

	@GetMapping({"wordCard"})
	public String wordCard(@RequestParam("bookId") Integer bookId, Model model) {

        BookDTO book = bookService.getBook(bookId);
        List<WordDTO> wordList = bookService.getWords(bookId);

        model.addAttribute("book", book);
        model.addAttribute("wordList", wordList);

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

    @GetMapping("studyCardPage")
    public String studyCardPage() {
        return "page/studyCardPage";
    }

}
