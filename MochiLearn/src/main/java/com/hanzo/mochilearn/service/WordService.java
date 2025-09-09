package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.TranslateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WordService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> translate(TranslateDTO translateDTO) {

        List<String> translatedWords = callDeepL(translateDTO);
        log.debug("[번역된 단어 목록]: {}", translatedWords);

        return translatedWords;
    }

    private List<String> callDeepL(TranslateDTO translateDTO) {
        String url = "https://api-free.deepl.com/v2/translate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "DeepL-Auth-Key " + "af87a0f0-fcef-482e-bd17-9239037b4862:fx");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        translateDTO.getWordList().forEach(word -> body.add("text", word));
        body.add("target_lang", "KO"); // 한국어로 번역
        body.add("source_lang", "JA"); // 원문이 일본어
        body.add("context", translateDTO.getContext());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        List<String> results = new ArrayList<>();
        if (response != null && response.containsKey("translations")) {
            List<Map<String, Object>> translations = (List<Map<String, Object>>) response.get("translations");
            for(Map<String, Object> translation : translations) {
                results.add((String) translation.get("text"));
            }
        }
        return results;
    }
}
