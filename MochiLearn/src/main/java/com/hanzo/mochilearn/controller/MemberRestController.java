package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.dto.card.CardDTO;
import com.hanzo.mochilearn.security.AuthenticatedUser;
import com.hanzo.mochilearn.service.CardService;
import com.hanzo.mochilearn.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Data
@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberRestController {

    private final CardService cardService;

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
}
