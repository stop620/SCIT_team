package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.controller.DuplicateUserIdException;
import com.hanzo.mochilearn.dto.member.MemberDTO;
import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    public void join(MemberDTO member) {

        log.debug("[service] joining processing...");

        Optional<MemberEntity> memberEntity = memberRepository.findByUserId(member.getUserId());
        if(memberEntity.isPresent()) {
            throw new DuplicateUserIdException("아이디 중복 오류");
        }
        MemberEntity entity = MemberEntity.builder()
                .userId(member.getUserId())
                .password(passwordEncoder.encode(member.getPassword()))
                .name(member.getName())
                .nickname(member.getNickname())
                .build();

        log.debug("[service] memberEntity : {}", entity);

        memberRepository.save(entity);
    }

/*    public boolean isUserIdExist(String userId) {

        Optional<MemberEntity> memberEntity = memberRepository.findByUserId(userId);

        return memberEntity.isPresent();
    }*/
}