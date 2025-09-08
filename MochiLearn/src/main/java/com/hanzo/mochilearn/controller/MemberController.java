package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.MemberDTO;
import com.hanzo.mochilearn.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService ms;

    @GetMapping("loginForm")
    public String loginForm() {
        return "member/loginForm";
    }

    @GetMapping("joinForm")
    public String joinForm() {
        return "member/joinForm";
    }

    @PostMapping("join")
    public String join(MemberDTO member) {
        log.debug("전달된 회원정보: {}", member);

        try {
            ms.join(member);
            log.debug("가입성공!");
            return "redirect:/";
        } catch (Exception e) {
            log.debug("가입실패..");
            return "member/joinForm";
        }

    }

}