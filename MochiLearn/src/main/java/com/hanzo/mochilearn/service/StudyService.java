package com.hanzo.mochilearn.service;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hanzo.mochilearn.dto.StudyDTO;
import com.hanzo.mochilearn.entity.StudyEntity;
import com.hanzo.mochilearn.repository.StudyRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional

public class StudyService {
	private final StudyRepository sr;
	
	public List<StudyDTO> getAllCards() {
	    return sr.findAll()
	             .stream()
	             .map(this::toDTO)
	             .collect(Collectors.toList());
	}

    private StudyDTO toDTO(StudyEntity entity) {
        return StudyDTO.builder()
                .cardId(entity.getCardId())
                .japanese(entity.getJapanese())
                .korean(entity.getKorean())
                .url(entity.getUrl())
                .timeline(entity.getTimeline())
                .level(entity.getLevel())
                .genre(entity.getGenre())
                .likeCount(entity.getLikeCount())
                .createdDate(entity.getCreatedDate())
                .build();
    }
    public List<StudyDTO> getPagedCards(String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StudyEntity> entities;
        if ("latest".equalsIgnoreCase(sort)) {
            entities = sr.findAllByOrderByCreatedDateDesc(pageable);
        } else { // 인기순 기본
            entities = sr.findAllByOrderByLikeCountDesc(pageable);
        }
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
