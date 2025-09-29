package com.hanzo.mochilearn.repository.card;

import com.hanzo.mochilearn.entity.card.SentenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentenceRepository extends JpaRepository<SentenceEntity, Integer> {
	List<SentenceEntity> findBySectionId(Integer sectionId);
}
