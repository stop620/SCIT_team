package com.hanzo.mochilearn.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.repository.CardRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class CardService {
    private final CardRepository cr;

    public List<CardDTO> getAllCards() {
        return cr.findAll()
                 .stream()
                 .map(this::toDTO)
                 .collect(Collectors.toList());
    }

    public CardDTO toDTO(CardEntity entity) {
        if (entity == null) return null;
        return CardDTO.builder()
                .cardId(entity.getCardId())
                .url(entity.getUrl())
                .title(entity.getTitle())
                .level(entity.getLevel())
                .like(entity.getLike())  // 필드명 맞춤
                .createdDate(entity.getCreatedDate())
                // 만약 memberId 포함 시, 아래 주석 해제하고 구현 필요
                //.memberId(entity.getMember() != null ? entity.getMember().getMemberId() : null)
                .build();
    }
    //페이지 불러오기
    public List<CardDTO> getPagedCards(String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardEntity> entityPage;

        if ("latest".equalsIgnoreCase(sort)) {
            entityPage = cr.findAllByOrderByCreatedDateDesc(pageable);
        } else { // 인기순 기본
            entityPage = cr.findAllByOrderByLikeDesc(pageable);
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
            entityPage = cr.findByTitleContainingIgnoreCaseOrderByCreatedDateDesc(search, pageable);
        } else {
            entityPage = cr.findByTitleContainingIgnoreCaseOrderByLikeDesc(search, pageable);
        }

        return entityPage.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
	//cardId 기준으로 일치하는 studyCard 확인
    public CardEntity getCardById(Integer cardId) {
        return cr.findById(cardId).orElse(null);
    }

	

	

}
