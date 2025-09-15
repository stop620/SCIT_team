package com.hanzo.mochilearn.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hanzo.mochilearn.entity.card.SentenceEntity;
import com.hanzo.mochilearn.repository.card.SentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SentenceService {
	 private final SentenceRepository sentenceRepository;
	
	
	 public List<SentenceEntity> getSentencesBySectionId(int sectionId) {
	        return sentenceRepository.findBySectionId(sectionId);
	    }
}
