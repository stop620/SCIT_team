package com.hanzo.mochilearn.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
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
                .sections(new ArrayList<>())
                .build();

        // "초급", "중급", "고급" 문자열을 숫자로 저장
        switch (cardDto.getLevel()) {
            case "초급": cardEntity.setLevel(1); break;
            case "중급": cardEntity.setLevel(2); break;
            case "고급": cardEntity.setLevel(3); break;
        }

        // TODO: 로그인 완성 시 member_id는 현재 로그인한 사용자 정보에서 가져와야 합니다
        // card.setMemberId( ... );

        log.debug("cardEntity: {}", cardEntity);

        CardEntity savedCard = cardRepository.save(cardEntity);

        log.info("Saved Card with ID: {}", savedCard);

        // Section 및 Sentence 엔티티 생성 및 저장
        if (cardDto.getSections() != null) {

            for (SectionDTO sectionDto : cardDto.getSections()) {

                log.debug("sectionDto: {}", sectionDto);

                SectionEntity section = SectionEntity.builder()
                        .startSeconds(sectionDto.getStartSeconds())
                        .endSeconds(sectionDto.getEndSeconds())
                        .sectionNum(sectionDto.getSectionNum())
                        .sentences(new ArrayList<>())
                        .build();

                savedCard.addSection(section); //카드 <> 섹션 연관 관계 설정
                SectionEntity savedSection = sectionRepository.save(section);

                log.info("  Saved Section : {}", savedSection);

                if (sectionDto.getSentences() != null) {
                    for (SentenceDTO sentenceDto : sectionDto.getSentences()) {

                        log.debug("sentenceDto: {}", sentenceDto);

                        SentenceEntity sentence = SentenceEntity.builder()
                                .sentenceIndex(sentenceDto.getSentenceIndex())
                                .time(sentenceDto.getTime())
                                .japanese(sentenceDto.getJapanese())
                                .korean(sentenceDto.getKorean())
                                .build();

                        savedSection.addSentence(sentence); // 섹션 <> 문장 연관관계 설정
                        sentenceRepository.save(sentence);
                    }
                    log.info("    Saved {} sentences for Section ID: {}", sectionDto.getSentences().size(), savedSection.getId());
                }
            }
        }
    }

    // 모든 카드 로드
    public List<CardDTO> getAllCards() {
    	
        return cardRepository.findAll().stream()
                .map(this::toSimpleDTO)
                .collect(Collectors.toList());
               
    }
    
    public CardDTO toSimpleDTO(CardEntity entity) {
    	List<SectionDTO> sections = entity.getSections().stream()
    	        .map(SectionDTO::toDTO)
    	        .collect(Collectors.toList());
    	
    	CardDTO dto = CardDTO.builder()
    			.id(entity.getId())
    			.title(entity.getTitle())
    			.url(entity.getUrl())
    			.level(entity.getLevel().toString())
    			.like(entity.getLike())
    			.createdDate(entity.getCreatedDate())
    			.memberId(entity.getMemberId())
    			.tag(entity.getTag())
    			.sections(sections)
    			.build();
    	
    	return dto;
    }
    //section값 없는거....
    public CardDTO toDTO(CardEntity entity) {
    	
    	
    	CardDTO dto = CardDTO.builder()
    			.id(entity.getId())
    			.title(entity.getTitle())
    			.url(entity.getUrl())
    			.level(entity.getLevel().toString())
    			.like(entity.getLike())
    			.createdDate(entity.getCreatedDate())
    			.memberId(entity.getMemberId())
    			.tag(entity.getTag())
    			.sections(null)
    			.build();
    	
    	return dto;
    }

    
    //페이지 불러오기
    public List<CardDTO> getPagedCards(String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardEntity> entityPage;

        if ("latest".equalsIgnoreCase(sort)) {
            entityPage = cardRepository.findAllByOrderByCreatedDateDesc(pageable);
        } else { // 인기순 기본
            entityPage = cardRepository.findAllByOrderByLikeDesc(pageable);
        }

        return entityPage.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    //제목으로 검색하기
    public List<CardDTO> searchCards(String sort, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardEntity> entityPage;

        if ("latest".equalsIgnoreCase(sort)) {
            entityPage = cardRepository.findByTitleContainingIgnoreCaseOrderByCreatedDateDesc(search, pageable);
        } else {
            entityPage = cardRepository.findByTitleContainingIgnoreCaseOrderByLikeDesc(search, pageable);
        }

        return entityPage.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    //cardId와 일치하는 데이터 가져오기
	public CardEntity findCardById(Integer cardId) {
		return cardRepository.findById(cardId).orElse(null);		
	}

    
    
}
