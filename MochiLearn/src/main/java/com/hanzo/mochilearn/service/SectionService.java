package com.hanzo.mochilearn.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hanzo.mochilearn.entity.SectionEntity;
import com.hanzo.mochilearn.repository.SectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SectionService {
	private final SectionRepository sectionRepository;


    public List<SectionEntity> getSectionsByCardId(int cardId) {
        return sectionRepository.findByCardId(cardId);
    }
}
