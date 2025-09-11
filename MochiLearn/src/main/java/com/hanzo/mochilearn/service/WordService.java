package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.word.TranslateDTO;
import com.hanzo.mochilearn.dto.word.WordSaveDTO;
import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.entity.word.Book;
import com.hanzo.mochilearn.entity.word.Word;
import com.hanzo.mochilearn.entity.word.WordBookMapEntity;
import com.hanzo.mochilearn.repository.MemberRepository;
import com.hanzo.mochilearn.repository.word.BookRepository;
import com.hanzo.mochilearn.repository.word.WordBookMapRepository;
import com.hanzo.mochilearn.repository.word.WordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class WordService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final BookRepository bookRepository;
    private final WordRepository wordRepository;
    private final WordBookMapRepository wordBookMapRepository;
    private final MemberRepository memberRepository;

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

    public Integer save(WordSaveDTO wordSaveDTO) {

        Book book = bookRepository.findById(wordSaveDTO.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("단어장 없음"));

        // 단어 db에 같은 단어가 있는지 확인
        Optional<Word> result = wordRepository.findByWordAndMeaningAndPos(wordSaveDTO.getWord(), wordSaveDTO.getMeaning(), wordSaveDTO.getPos());
        Word word = new Word();
        if (result.isPresent()) { // 단어 db에 같은 단어가 있을경우 불러와서 연결
            word = result.get();

            //단어장에 이미 저장되어있는지 확인
            WordBookMapEntity isExist = wordBookMapRepository.findByBookIdAndWordId(book.getId(), word.getId());
            log.debug("[단어 저장여부 확인]: {}", isExist);
            if (isExist != null) {  // 이미 저장되어 있을 때
                log.debug("[단어 저장] : 이미 단어장에 저장된 단어");
                return -1;  // 반환 코드
            }
        } else {    // 단어 db에 없을경우 추가 후 연결
            word = Word.builder()
                    .word(wordSaveDTO.getWord())
                    .meaning(wordSaveDTO.getMeaning())
                    .pos(wordSaveDTO.getPos())
                    .build();

        }
        wordRepository.save(word);
        log.debug("[저장된 단어]: {}", word);

        WordBookMapEntity mapEntity = WordBookMapEntity.builder()
                .word(word)
                .book(book)
                .build();

        wordBookMapRepository.save(mapEntity);
        log.debug("[저장된 단어장]: {}", wordSaveDTO.getBookId());
        log.debug("[단어-단어장 연결]: {}", mapEntity);

        return wordSaveDTO.getBookId();
    }
    public void removeWord(Integer bookId, Integer wordId, Integer memberId) {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(()->new EntityNotFoundException("멤버 없음"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("단어장 없음"));

        if(book.getMember().getId() == member.getId()) {
            WordBookMapEntity mapEntity = wordBookMapRepository.findByBookIdAndWordId(bookId, wordId);
            log.debug("[삭제할 연결 엔티티] : mapEntity: {}", mapEntity);
            wordBookMapRepository.delete(mapEntity);

            log.debug("[단어장 단어 삭제]: 단어장 {}에서 단어{} 삭제 성공.", bookId, wordId);
        }
    }
}
