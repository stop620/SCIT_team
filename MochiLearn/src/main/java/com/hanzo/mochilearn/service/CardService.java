package com.hanzo.mochilearn.service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hanzo.mochilearn.dto.word.TokenDTO;
import com.hanzo.mochilearn.entity.*;
import com.hanzo.mochilearn.entity.card.CardEntity;
import com.hanzo.mochilearn.entity.card.LikeEntity;
import com.hanzo.mochilearn.entity.card.SectionEntity;
import com.hanzo.mochilearn.entity.card.SentenceEntity;
import com.hanzo.mochilearn.entity.word.Token;
import com.hanzo.mochilearn.repository.*;
import com.hanzo.mochilearn.repository.card.CardRepository;
import com.hanzo.mochilearn.repository.card.LikeRepository;
import com.hanzo.mochilearn.repository.card.SectionRepository;
import com.hanzo.mochilearn.repository.card.SentenceRepository;
import com.hanzo.mochilearn.repository.word.TokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanzo.mochilearn.dto.card.CardDTO;
import com.hanzo.mochilearn.dto.card.SectionDTO;
import com.hanzo.mochilearn.dto.card.SentenceDTO;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
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
    private final MemberRepository memberRepository;
    private final LikeRepository likeRepository;
    private final TokenRepository tokenRepository;

    // 학습카드 DB에 저장
    public Integer save(CardDTO cardDto, int memberId) throws Exception {

        CardEntity cardEntity = CardEntity.builder()
                .title(cardDto.getTitle())
                .url(cardDto.getUrl())
                .tag(cardDto.getTag())
                .memberId(memberId)
                .sections(new ArrayList<>())
                .like(cardDto.getLike())
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

                log.info("Saved Section : {}", savedSection);

                if (sectionDto.getSentences() != null) {
                    for (SentenceDTO sentenceDto : sectionDto.getSentences()) {

                        log.debug("sentenceDto: {}", sentenceDto);

                        SentenceEntity sentence = SentenceEntity.builder()
                                .sentenceIndex(sentenceDto.getSentenceIndex())
                                .time(sentenceDto.getTime())
                                .japanese(sentenceDto.getJapanese())
                                .korean(sentenceDto.getKorean())
                                .tokens(new ArrayList<>())
                                .build();

                        savedSection.addSentence(sentence); // 섹션 <> 문장 연관관계 설정
                        SentenceEntity savedSentence = sentenceRepository.save(sentence);

                        if(sentenceDto.getJapaneseTokens() != null) {
                            for(TokenDTO token : sentenceDto.getJapaneseTokens()) {

                                log.debug("sentenceToken: {}", token);

                                Token tokenEntity = Token.builder()
                                        .tokenIndex(token.getIndex())
                                        .surface(token.getSurface())
                                        .partOfSpeech(token.getPos())
                                        .baseForm(token.getBase())
                                        .reading(token.getReading())
                                        .build();

                                savedSentence.addToken(tokenEntity);
                                tokenRepository.save(tokenEntity);
                            }
                        }
                    }
                    log.info("    Saved {} sentences for Section ID: {}", sectionDto.getSentences().size(), savedSection.getId());
                }
            }
            return cardEntity.getId();
        } else {
            throw new Exception();
        }
    }

    // 모든 카드 로드
    public List<CardDTO> getAllCards() {
    	
        return cardRepository.findAll().stream()
                .map(this::toSimpleDTO)
                .collect(Collectors.toList());
               
    }

    // 멤버의 모든 카드 로드(섹션 데이터 제외)
    public List<CardDTO> getAllCards (Integer memberId) throws EntityNotFoundException {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(()-> new EntityNotFoundException("Member with ID: " + memberId + " not found"));

        return cardRepository.findAllByMemberId(memberId).stream()
                .map(this::toDTO)
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
    //cardId와 일치하는 데이터 가져오기
  	public CardEntity findCardById(Integer cardId) {
  		return cardRepository.findById(cardId).orElse(null);		
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

    //타이틀, 닉네임으로 검색하기
    public List<CardDTO> searchCardsByTitleOrNickname(String sort, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);

        // 1. 닉네임에 포함되는 멤버 리스트 조회
        List<MemberEntity> members = memberRepository.findByNicknameContainingIgnoreCase(search);
        List<Integer> memberIds = members.stream()
                                         .map(MemberEntity::getId)
                                         .collect(Collectors.toList());

        // 2. 제목 포함 또는 멤버 ID 중 하나 일치하는 카드 검색
        Page<CardEntity> entityPage;
        if ("latest".equalsIgnoreCase(sort)) {
            entityPage = cardRepository.findByTitleContainingIgnoreCaseOrMemberIdInOrderByCreatedDateDesc(
                search, memberIds, pageable);
        } else {
            entityPage = cardRepository.findByTitleContainingIgnoreCaseOrMemberIdInOrderByLikeDesc(
                search, memberIds, pageable);
        }

        return entityPage.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    public Page<CardDTO> searchCardsByTags(List<String> tags, int page, int size, String sort) {
        Sort sortOrder;

        if ("latest".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "createdDate");
        } else { // 기본 인기순
            sortOrder = Sort.by(Sort.Direction.DESC, "like");
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Specification<CardEntity> spec = (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) {
                return cb.conjunction();
            }
            Predicate predicate = cb.disjunction();
            for (String tag : tags) {
                predicate = cb.or(predicate,
                        cb.like(cb.lower(root.get("tag")), "%" + tag.toLowerCase() + "%"));
            }
            return predicate;
        };

        Page<CardEntity> cardPage = cardRepository.findAll(spec, pageable);

        return cardPage.map(this::toDTO);
    }
    public boolean deleteCardById(Integer cardId) {
        if (cardRepository.existsById(cardId)) {
            cardRepository.deleteById(cardId);
            return true;
        }
        return false;
    }

    public boolean toggleLike(Integer memberId, Integer cardId, Boolean like) {
        log.debug("toggleLike 호출 - memberId: {}, cardId: {}, like 요청값: {}", memberId, cardId, like);

        CardEntity card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));
        log.debug("조회된 카드: id = {}, 현재 좋아요 수 = {}", card.getId(), card.getLike());

        Optional<LikeEntity> existingLike = likeRepository.findByMemberIdAndCardId(memberId, cardId);
        log.debug("기존 좋아요 여부: {}", existingLike.isPresent());

        if (like) {
            if (existingLike.isEmpty()) {
                // 새로 좋아요 추가
                LikeEntity likeEntity = LikeEntity.builder()
                    .memberId(memberId)
                    .cardId(cardId)
                    .likeTime(LocalDateTime.now())
                    .build();
                likeRepository.save(likeEntity);
                card.setLike(card.getLike() + 1);
                cardRepository.save(card);
                log.debug("좋아요 추가 완료 - 좋아요 수 증가 후: {}", card.getLike());
            } else {
                log.debug("이미 좋아요가 존재하여 추가하지 않음");
            }
            return true; // 최종 상태는 liked
        } else {
            if (existingLike.isPresent()) {
                likeRepository.delete(existingLike.get());
                card.setLike(card.getLike() - 1);
                cardRepository.save(card);
                log.debug("좋아요 삭제 완료 - 좋아요 수 감소 후: {}", card.getLike());
            } else {
                log.debug("좋아요가 존재하지 않아 삭제하지 않음");
            }
            return false; // 최종 상태는 unliked
        }
    }

    // memberId가 좋아요 누른 카드 목록 반환하는 메소드
    public List<CardDTO> getMemberLikeCards(Integer memberId) {

        log.debug("[멤버 좋아요 카드] memberId {}의 좋아요 목록 검색", memberId);

        List<LikeEntity> likes = likeRepository.findAllByMemberId(memberId);
        List<Integer> cardIds = likes.stream()
                .map(likeEntity -> likeEntity.getCardId()).collect(Collectors.toList());

        List<CardEntity> cardEntities = cardRepository.findAllById(cardIds);

        return cardEntities.stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public CardEntity getCardById(Integer cardId) {
        return cardRepository.findById(cardId)
                .orElse(null);  // ID에 해당하는 카드가 없으면 null 반환
    }
}
