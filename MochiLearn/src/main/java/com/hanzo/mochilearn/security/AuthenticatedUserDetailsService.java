package com.hanzo.mochilearn.security;

import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.repo.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 인증 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticatedUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        log.info("로그인 시도 : {}", id);
        MemberEntity memberEntity = memberRepository.findByUserId(id)
                .orElseThrow(() -> new UsernameNotFoundException(id + " : 없는 ID입니다."));

        log.debug("조회정보 : {}", memberEntity);

        // 인증정보 생성
        AuthenticatedUser user = AuthenticatedUser.builder()
                .id(memberEntity.getUserId())
                .password(memberEntity.getPassword())
                .name(memberEntity.getName())
                .role(memberEntity.getRole())
                .build();
		
		log.debug("인증정보 : {}", user);
	
		return user;
    }
}