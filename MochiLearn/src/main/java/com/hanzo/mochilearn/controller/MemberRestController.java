package com.hanzo.mochilearn.controller;

import com.hanzo.mochilearn.security.AuthenticatedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Data
@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberRestController {

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
}
