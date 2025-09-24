package com.hanzo.mochilearn.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.repository.MemberRepository;
import com.hanzo.mochilearn.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService{

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final MemberRepository memberRepository;

    // 로그인 처리 메인 메서드
    @Transactional
    public void loginUser(String idTokenString, HttpServletRequest request) throws GeneralSecurityException, IOException {
        // ID 토큰으로 사용자 정보 조회/생성
        MemberEntity member = processAndGetUser(idTokenString);

        // AuthenticatedUser Principal 생성
        AuthenticatedUser principal = AuthenticatedUser.builder()
                .memberId(member.getId())
                .id(member.getUserId())
                .password(member.getPassword())
                .name(member.getName())
                .role(member.getRole().name())
                .build();
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name()));

        // 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        // SecurityContext 생성 및 Authentication 설정
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // SecurityContext를 세션에 저장
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }

    private MemberEntity processAndGetUser(String idTokenString) throws GeneralSecurityException, IOException {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken == null) {
            throw new IllegalArgumentException("Invalid ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        return saveOrUpdate(email, name);
    }


    // DB에 사용자를 저장하거나 업데이트하는 메서드
    private MemberEntity saveOrUpdate(String email, String name) {
        // 이메일(userId)로 사용자 조회
        Optional<MemberEntity> memberOptional = memberRepository.findByUserId(email);

        MemberEntity member;
        if (memberOptional.isPresent()) {
            // 이미 가입된 회원일 경우 (로그인)
            member = memberOptional.get();
            // 이름이 변경되었을 수 있으니 업데이트
            member.setName(name);
            // 마지막 로그인 시간 업데이트
            member.setLastLoginDate(LocalDateTime.now());
        } else {
            // 처음 로그인하는 회원일 경우 (신규 가입)
            member = MemberEntity.builder()
                    .userId(email) // userId는 이메일로 설정
                    .name(name)
                    // password는 소셜 로그인이라 불필요. nullable=false이므로 임의의 값 저장
                    .password(UUID.randomUUID().toString())
                    // nickname은 nullable=false. 우선 이름으로 설정 후 사용자가 변경하도록 유도
                    .nickname(name)
                    .role(MemberEntity.Role.USER) // 기본 역할은 USER
                    .lastLoginDate(LocalDateTime.now())
                    .build();
        }

        // 트랜잭션 내에 있으므로 메서드 종료 시 자동으로 DB에 저장/업데이트 됩니다.
        return memberRepository.save(member);
    }
}
