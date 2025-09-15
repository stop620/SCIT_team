package com.hanzo.transcribeserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanzo.transcribeserver.dict.*;
import com.hanzo.transcribeserver.dto.DictionaryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JmdictService {

    private final DeepLService deepLService;
    private final PosConverter posConverter;

    // JMDict 데이터
    private List<JmdictWord> allWords;

    @Autowired
    private ResourceLoader resourceLoader;

    // 이 메서드가 완료되어야 서비스가 준비됨
    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:jmdict.json");
            InputStream inputStream = resource.getInputStream();

            ObjectMapper mapper = new ObjectMapper();
            List<JmdictWord> loadedWords = mapper.readValue(
                    inputStream,
                    mapper.getTypeFactory().constructCollectionType(List.class, JmdictWord.class)
            );

            this.allWords = loadedWords;

            System.out.println("총 " + this.allWords.size() + "개의 단어 항목을 성공적으로 불러왔습니다. 🎉");

        } catch (IOException e) {
            System.err.println("JMDict 데이터 로딩 실패: " + e.getMessage());
            this.allWords = Collections.emptyList(); // 오류 시 빈 리스트로 초기화
        }
    }

    public Optional<DictionaryResponseDTO> findWord(String query) {

        // 단어를 검색
        Optional<JmdictWord> word = findWordByQuery(query);
        word = translateToKorean(word);
        if (word.isPresent()) {
            return word.map(this::convertToDto);
        } else {
            return Optional.empty();
        }
    }

    // 단어를 검색하는 메서드
    public Optional<JmdictWord> findWordByQuery(String query) {
        // 모든 엔트리를 순회하며 필드가 일치하는 항목을 반환
        return allWords.stream()
                .filter(word -> matchesQuery(word, query))
                .findFirst();
    }

    // 단어가 쿼리와 일치하는지 판단하는 헬퍼 메서드
    private boolean matchesQuery(JmdictWord word, String query) {
        // kanji 또는 kana 중 하나라도 일치하면 true
        return matchesKanji(word, query) || matchesKana(word, query);
    }

    // kanji(한자) 리스트에서 쿼리가 일치하는지 확인
    private boolean matchesKanji(JmdictWord word, String query) {
        return word.getKanji() != null &&
                word.getKanji().stream().anyMatch(k -> k.getText().equals(query));
    }

    // kana(히라가나) 리스트에서 쿼리가 일치하는지 확인
    private boolean matchesKana(JmdictWord word, String query) {
        return word.getKana() != null &&
                word.getKana().stream().anyMatch(k -> k.getText().equals(query));
    }

    // 검색한 단어를 영어 설명을 한국어로 번역하는 메서드
    public Optional<JmdictWord> translateToKorean(Optional<JmdictWord> wordOptional) {

        if (wordOptional.isPresent()) {

            JmdictWord word = wordOptional.get();

            // 단어의 의미
            Sense sense = word.getSense();

            String japaneseOriginal = word.getKana().stream().findFirst().map(Kana::getText).orElse(null);

            // 품사 변환
            sense.setPartOfSpeech(posConverter.convertToKorean(sense.getPartOfSpeech()));

            String jpSentence = sense.getExamples().get(0).getSentences().get(0).getText();
            // 뜻 번역
            if (japaneseOriginal != null) {
                String directTranslation = deepLService.translate(japaneseOriginal, "JA", "KO");
                Gloss newGloss = new Gloss();
                newGloss.setText(directTranslation);
                sense.setGloss(List.of(newGloss));
            }

            // 예문 번역
            sense.getExamples().stream()
                    .filter(ex -> ex.getSentences() != null && ex.getSentences().size() > 1)
                    .forEach(ex -> {
                        // String jpSentence = ex.getSentences().get(0).getText();
                        ex.getSentences().get(1).setText(deepLService.translate(jpSentence, "JA", "KO", jpSentence));
                    });

            return Optional.of(word); // 번역이 완료된 단어 객체를 반환
        }
        return Optional.empty();
    }

    private DictionaryResponseDTO convertToDto(JmdictWord word) {
        Sense sense = word.getSense();

        // 1. kanji 리스트에서 text만 추출하여 List<String>으로 변환
        List<String> kanjiList = word.getKanji().stream()
                .map(Kanji::getText)
                .toList();

        // 2. kana 리스트에서 text만 추출
        List<String> kanaList = word.getKana().stream()
                .map(Kana::getText)
                .toList();

        // 3. partOfSpeech는 이미 List<String>이므로 그대로 사용
        List<String> posList = sense.getPartOfSpeech();

        // 4. gloss 리스트에서 text만 추출
        List<String> glossList = sense.getGloss().stream()
                .map(Gloss::getText)
                .toList();

        // 5. examples 리스트에서 모든 sentence의 text를 하나의 List<String>으로 추출
        List<String> exampleList = sense.getExamples().stream()
                .flatMap(example -> example.getSentences().stream()) // 모든 sentence들을 하나로 합침
                .map(Sentence::getText)
                .toList();

        // 최종 SimpleWordDto 레코드 생성
        return new DictionaryResponseDTO(kanjiList, kanaList, posList, glossList, exampleList);
    }
}