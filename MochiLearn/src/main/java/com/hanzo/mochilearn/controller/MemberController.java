package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService jarvis;

    @GetMapping("loginForm")
    public String login() {
        return "member/loginForm";
    }

    @GetMapping("joinForm")
    public String joinForm() {
        return "redirect:/";
    }
}