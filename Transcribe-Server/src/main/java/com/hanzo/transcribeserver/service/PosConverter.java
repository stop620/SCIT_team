package com.hanzo.transcribeserver.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PosConverter {

    // 품사 태그를 한국식 표현으로 매핑
    private static final Map<String, String> POS_MAP;

    static {
        // 주요 품사 태그들 매핑
        POS_MAP = Map.ofEntries(
                // ### 동사 (Verb) ###
                Map.entry("v1", "1단 활용 동사"),
                Map.entry("v1-s", "1단 활용 동사 (특수)"),
                Map.entry("v5u", "5단 활용 동사 (う)"),
                Map.entry("v5k", "5단 활용 동사 (く)"),
                Map.entry("v5g", "5단 활용 동사 (ぐ)"),
                Map.entry("v5s", "5단 활용 동사 (す)"),
                Map.entry("v5t", "5단 활용 동사 (つ)"),
                Map.entry("v5n", "5단 활용 동사 (ぬ)"),
                Map.entry("v5b", "5단 활용 동사 (ぶ)"),
                Map.entry("v5m", "5단 활용 동사 (む)"),
                Map.entry("v5r", "5단 활용 동사 (る)"),
                Map.entry("vs", "する 동사"),
                Map.entry("vs-s", "する 동사 (특수)"),
                Map.entry("vk", "くる 동사"),
                Map.entry("vz", "ずる 동사 (문어체)"),
                Map.entry("vi", "자동사"),
                Map.entry("vt", "타동사"),
                Map.entry("aux-v", "보조 동사"),

                // ### 형용사 (Adjective) ###
                Map.entry("adj-i", "い형용사"),
                Map.entry("adj-na", "な형용사"),
                Map.entry("adj-no", "の형용사"),
                Map.entry("adj-pn", "관체사"),
                Map.entry("adj-t", "たる 형용사"),
                Map.entry("adj-f", "명사·어간 형용사"),
                Map.entry("aux-adj", "보조 형용사"),

                // ### 명사 (Noun) ###
                Map.entry("n", "명사"),
                Map.entry("n-adv", "부사적 명사"),
                Map.entry("n-suf", "명사형 접미사"),
                Map.entry("n-pref", "명사형 접두사"),
                Map.entry("n-t", "시간 명사"),
                Map.entry("n-pr", "고유 명사"),
                Map.entry("pn", "대명사"),

                // ### 부사 (Adverb) ###
                Map.entry("adv", "부사"),
                Map.entry("adv-to", "と 부사"),

                // ### 기타 품사 ###
                Map.entry("conj", "접속사"),
                Map.entry("prt", "조사"),
                Map.entry("suf", "접미사"),
                Map.entry("pref", "접두사"),
                Map.entry("int", "감동사"),
                Map.entry("exp", "표현"),
                Map.entry("num", "수사"),
                Map.entry("ctr", "조수사 (수량 단위)"),
                Map.entry("unc", "미분류"),

                // ### 불규칙 활용 ###
                Map.entry("v5r-i", "불규칙 5단 활용 동사"),
                Map.entry("v-aru", "ある 동사"),
                Map.entry("v-suru", "する 동사"),
                Map.entry("adj-ix", "良い·いい 형용사"),
                Map.entry("iK", "불규칙 くる 동사"),
                Map.entry("iA", "불규칙 ある 형용사")
        );
    }

    /**
     * 품사 코드 리스트를 한국어 설명 리스트로 변환
     * @param codes 변환할 품사 코드 리스트 (예: ["v5r", "vi"])
     * @return 변환된 한국어 설명 리스트 (예: ["5단 활용 동사", "자동사"])
     */
    public List<String> convertToKorean(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return codes;
        }

        return codes.stream()
                // 각 코드를 POS_MAP에서 찾아 변환
                // 만약 맵에 없는 코드일 경우, 원본 코드를 그대로 반환 (getOrDefault)
                .map(code -> POS_MAP.getOrDefault(code, code))
                .collect(Collectors.toList());
    }
}