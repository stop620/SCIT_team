package com.hanzo.transcribeserver.service;

import com.deepl.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeepLService {

    // DeepL 라이브러리 객체
    private final Translator translator;

    /**
     * 서비스가 생성될 때 application.properties에서 API 키를 읽어와 Translator 객체를 초기화합니다.
     */
    public DeepLService(@Value("${deepl.api.key}") String apiKey) {
        this.translator = new Translator(apiKey);
    }

    /**
     * 텍스트를 번역하는 메서드
     * @param text 번역할 원본 텍스트
     * @param targetLang 목표 언어 코드 (예: "KO")
     * @return 번역된 텍스트. 실패 시 원본 텍스트 반환.
     */
    public String translate(String text, String sourceLang, String targetLang, String context) {
        // 번역할 텍스트가 없으면 그대로 반환
        if (text == null || text.isBlank()) {
            return text;
        }

        try {
            // 번역 옵션 객체
            TextTranslationOptions options = new TextTranslationOptions();
            options.setModelType("quality_optimized");
            if (context != null && !context.isBlank()) {
                options.setContext(context);
            }
            log.debug("[Translate Context]: {}", options.getContext());
            // 라이브러리의 번역 함수 호출 (소스 언어는 null로 두면 자동 감지)
            TextResult result = translator.translateText(text, sourceLang, targetLang, options);
            return result.getText();

        } catch (DeepLException | InterruptedException e) {
            System.err.println("DeepL API 번역 중 오류 발생: " + e.getMessage());
            // 에러 발생 시 원본 텍스트를 그대로 반환하여 서비스 장애를 최소화
            Thread.currentThread().interrupt(); // InterruptedException 발생 시 스레드 상태 복원
            return text;
        }
    }

    // context가 없는 경우를 위한 오버로딩
    public String translate(String text, String sourceLang, String targetLang) {
        return this.translate(text, sourceLang, targetLang, null);
    }
}