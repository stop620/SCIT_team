package com.hanzo.mochilearn.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 시큐리티 환경설정
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {
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

            , "/api/study/load"     // 카드 목록 불러오기 api
            , "/api/study/card"     // 카드 정보 불러오기 api


            /*
            , "/api/study/load"     // 카드 목록 api
            , "/api/study/card"     // 카드 세부 요청 api
            , "/api/quiz/**"           // 퀴즈 문제 요청 api

            , "/api/word/**"
            , "/member/**"*/
    };

    @Bean
    protected SecurityFilterChain config(HttpSecurity http) throws Exception {
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