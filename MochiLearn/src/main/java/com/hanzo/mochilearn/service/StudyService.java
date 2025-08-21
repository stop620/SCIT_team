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

    public StudyDTO toDTO(StudyEntity entity) {
        if (entity == null) return null;
        return StudyDTO.builder()
                .cardId(entity.getCardId())
                .url(entity.getUrl())
                .title(entity.getTitle())
                .level(entity.getLevel())
                .genre(entity.getGenre())
                .like(entity.getLike())  // 필드명 맞춤
                .createdDate(entity.getCreatedDate())
                // 만약 memberId 포함 시, 아래 주석 해제하고 구현 필요
                //.memberId(entity.getMember() != null ? entity.getMember().getMemberId() : null)
                .build();
    }

    public List<StudyDTO> getPagedCards(String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StudyEntity> entityPage;

        if ("latest".equalsIgnoreCase(sort)) {
            entityPage = sr.findAllByOrderByCreatedDateDesc(pageable);
        } else { // 인기순 기본
            entityPage = sr.findAllByOrderByLikeDesc(pageable);
        }

        return entityPage.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public StudyEntity getCardById(Long cardId) {
        return sr.findById(cardId).orElse(null);
    }
}
