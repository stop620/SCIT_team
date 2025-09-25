package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.member.MemberDTO;
import com.hanzo.mochilearn.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@RequestMapping("member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("loginForm")
    public String loginForm() {
        return "member/loginForm";
    }

    @GetMapping("joinForm")
    public String joinForm() {
        return "member/joinForm";
    }

/*    @PostMapping("login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData,
                                                     @RequestParam(value = "redirect", required = false) String redirectUrl,
                                                     HttpServletRequest request) {

        String userId = loginData.get("id");
        String password = loginData.get("password");
        log.debug("로그인 시도: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 멤버 db에 아이디가 존재하는지 확인
            if (!memberService.isUserIdExist(userId)) {
                log.debug("로그인 실패: 없는 아이디.");
                response.put("success", false);
                response.put("message", "존재하지 않는 아이디입니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 인증 매니저를 사용하여 인증을 시도합니다.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userId, password)
            );

            // 인증 성공 시 SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("로그인 성공! 사용자: {}", authentication.getName());
            response.put("success", true);
            response.put("message", "로그인 성공!");
            // 이전 페이지로 리디렉션 시킬 주소
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                // URL 디코딩
                String decodedUrl = URLDecoder.decode(redirectUrl, StandardCharsets.UTF_8);
                response.put("redirectUrl", decodedUrl);
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadCredentialsException e) {
            log.debug("로그인 실패: 비밀번호 오류", e.getMessage());
            response.put("success", false);
            response.put("message", "비밀번호가 일치하지 않습니다.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("로그인 실패: 알 수 없는 오류", e);
            response.put("success", false);
            response.put("message", "로그인 중 오류가 발생했습니다. 다시 시도해 주세요.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

    @PostMapping("join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> join(@RequestBody MemberDTO member) {
        log.debug("전달된 회원정보: {}", member);

        Map<String, Object> response = new HashMap<>();

        try {
            memberService.join(member);
            log.debug("가입 성공!");
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (DuplicateUserIdException e) {
            log.debug("가입 실패... {}", e.getMessage());
            response.put("success", false);
            response.put("message", "이미 사용중인 이메일입니다.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict 상태 코드 반환
        } catch (Exception e) {
            log.debug("가입 실패... {}", e.getMessage());
            response.put("success", false);
            response.put("message", "가입에 실패했습니다. 다시 시도해 주세요.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error 상태 코드 반환
        }
    }

/*    @PostMapping("logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logoutApi(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        return ResponseEntity.ok(responseBody);
    }*/

}