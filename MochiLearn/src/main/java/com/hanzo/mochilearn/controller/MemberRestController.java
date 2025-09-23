package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.card.CardDTO;
import com.hanzo.mochilearn.dto.quiz.QuizLog;
import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.security.AuthenticatedUser;
import com.hanzo.mochilearn.service.CardService;
import com.hanzo.mochilearn.service.CustomOAuth2UserService;
import com.hanzo.mochilearn.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Data
@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberRestController {

    private final CardService cardService;
    private final QuizService quizService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserSessionResponse {
        private MemberInfo member;
        private boolean loggedIn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class MemberInfo {
        private Integer id;
        private String userId;
        private String name;
    }

    @GetMapping("/api/user/session")
    public ResponseEntity<UserSessionResponse> getUserSession(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.ok(new UserSessionResponse(null, false));
        }

        MemberInfo memberInfo = new MemberInfo(user.getMemberId(), user.getId(), user.getName());

        return ResponseEntity.ok(new UserSessionResponse(memberInfo, true));
    }

    // 마이페이지 유저Id의 카드 데이터 주는 api
    @GetMapping("/api/member/mycard")
    public List<CardDTO> getMyCards(@RequestParam("id") Integer memberId) {

        List<CardDTO> memberCardList = cardService.getAllCards(memberId);
        return memberCardList;
    }

    // 마이페이지 유저Id의 좋아요 한 카드 데이터 주는 api
    @GetMapping("/api/member/likecard")
    public List<CardDTO> getLikecards(@RequestParam("id") Integer memberId) {

        List<CardDTO> memberCardList = cardService.getMemberLikeCards(memberId);

        return memberCardList;
    }

    // 마이페이지 유저 Id의 퀴즈 결과 데이터 주는 api
    @GetMapping("/api/member/quizlogs")
    public List<QuizLog> getQuizAttempts(@RequestParam("id") Integer memberId) {

        List<QuizLog> memberQuizLogs = quizService.getMemberQuizSessions(memberId);

        return memberQuizLogs;
    }

    // OAuth ID 토큰을 받기 위한 임시 DTO
    @Getter
    @Setter
    public static class IdTokenRequestDTO {
        private String idToken;
    }

    @PostMapping("/api/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody IdTokenRequestDTO requestDTO, HttpServletRequest request) {
        try {
            customOAuth2UserService.loginUser(requestDTO.getIdToken(), request);

            log.debug("Google login successful");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // 토큰 검증 실패 또는 다른 예외 발생 시
            log.debug("Google login failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
