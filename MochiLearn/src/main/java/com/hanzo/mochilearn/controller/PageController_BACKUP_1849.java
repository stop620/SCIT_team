package com.hanzo.mochilearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("write")
    public String writePage() {
        return "page/writePage";
    }
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    
    @GetMapping("studyCardPage")
    public String studyCardPage(){
    	return "page/studyCardPage";
=======
=======
>>>>>>> 5390e62 (디자인 테스트 페이지 추가)
=======
>>>>>>> 5390e62 (디자인 테스트 페이지 추가)

    @GetMapping("/test/home")
    public String testHome() {
        return "page/testHome";
    }

    @GetMapping("/test/best")
    public String testBest() {
        return "page/testBest";
    }

    @GetMapping("/test/study")
    public String testStudy() {
        return "page/testStudy";
<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> 5390e62 (디자인 테스트 페이지 추가)
=======
>>>>>>> 5390e62 (디자인 테스트 페이지 추가)
=======
>>>>>>> 5390e62 (디자인 테스트 페이지 추가)
    }
}
