package com.hanzo.mochilearn.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.hanzo.mochilearn.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

/**
 * 시큐리티 환경설정
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    //로그인 없이 접근 가능 경로
    private static final String[] PUBLIC_URLS = {
            "/"                     //root
            , "/img/**"             //이미지 경로
            , "/css/**"             //CSS파일들
            , "/js/**"              //JavaSCript 파일들

            , "/member/joinForm"    //회원가입
            , "/member/join"
            , "/member/loginForm"

            , "/page/best"          // 인기/최신 카드 페이지
            , "/page/study"         // 전체 카드 페이지
            , "/page/studyCard"     // 학습 카드 페이지

            , "/api/user/session"
            , "/api/study/load"     // 카드 목록 불러오기 api
            , "/api/study/card"     // 카드 정보 불러오기 api
            , "/api/study/filterByTags" // 카드 정보 필터

            , "/api/auth/google"        // 구글로그인 api


            /*
            , "/api/study/load"     // 카드 목록 api
            , "/api/study/card"     // 카드 세부 요청 api
            , "/api/quiz/**"           // 퀴즈 문제 요청 api

            , "/api/word/**"
            , "/member/**"*/
    };

    @Bean
    protected SecurityFilterChain config(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService) throws Exception {
        http
            .authorizeHttpRequests(author -> author
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
//            .httpBasic(Customizer.withDefaults())
            .formLogin(formLogin -> formLogin
                    .loginPage("/member/loginForm")
                    .usernameParameter("id")
                    .passwordParameter("password")
                    .loginProcessingUrl("/member/login")
                    .defaultSuccessUrl("/", true)
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/member/logout")
                    .logoutSuccessUrl("/")
            );

        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

}