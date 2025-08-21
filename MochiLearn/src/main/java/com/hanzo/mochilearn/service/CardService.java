package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.dto.SectionDTO;
import com.hanzo.mochilearn.dto.SentenceDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.SectionEntity;
import com.hanzo.mochilearn.entity.SentenceEntity;
import com.hanzo.mochilearn.repository.CardRepository;
import com.hanzo.mochilearn.repository.SectionRepository;
import com.hanzo.mochilearn.repository.SentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final SectionRepository sectionRepository;
    private final SentenceRepository sentenceRepository;

    // 학습카드 DB에 저장
    public void save(CardDTO cardDto, int memberId) throws Exception {

        CardEntity cardEntity = CardEntity.builder()
                .title(cardDto.getTitle())
                .url(cardDto.getUrl())
                .tag(cardDto.getTag())
                .memberId(memberId)

                .build();

        // "초급", "중급", "고급" 문자열을 숫자로 저장
        switch (cardDto.getLevel()) {
            case "초급": cardEntity.setLevel(1); break;
            case "중급": cardEntity.setLevel(2); break;
            case "고급": cardEntity.setLevel(3); break;
        }

        // TODO: member_id는 현재 로그인한 사용자 정보에서 가져와야 합니다.
        // card.setMemberId( ... );

        log.debug("cardEntity: {}", cardEntity);

        CardEntity savedCard = cardRepository.save(cardEntity);

        log.info("Saved Card with ID: {}", savedCard);

        // 2. Section 및 Sentence 엔티티 생성 및 저장
        if (cardDto.getSections() != null) {

            for (SectionDTO sectionDto : cardDto.getSections()) {

                log.debug("sectionDto: {}", sectionDto);

                SectionEntity section = SectionEntity.builder()
                        .card(savedCard)
                        .startSeconds(sectionDto.getStartSeconds())
                        .endSeconds(sectionDto.getEndSeconds())
                        .sectionNum(sectionDto.getSectionNum())
                        .build();

                SectionEntity savedSection = sectionRepository.save(section);

                log.info("  Saved Section : {}", savedSection);

                if (sectionDto.getSentences() != null) {
                    for (SentenceDTO sentenceDto : sectionDto.getSentences()) {

                        log.debug("sentenceDto: {}", sentenceDto);

                        SentenceEntity sentence = SentenceEntity.builder()
                                .section(savedSection)
                                .sentenceIndex(sentenceDto.getSentenceIndex())
                                .time(sentenceDto.getTime())
                                .japanese(sentenceDto.getJapanese())
                                .korean(sentenceDto.getKorean())
                                .build();

                        sentenceRepository.save(sentence);
                    }
                    log.info("    Saved {} sentences for Section ID: {}", sectionDto.getSentences().size(), savedSection.getId());
                }
            }
        }
    }
}
