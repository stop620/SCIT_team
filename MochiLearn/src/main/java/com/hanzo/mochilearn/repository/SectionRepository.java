package com.hanzo.mochilearn.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.SectionEntity;

@Repository
public interface SectionRepository extends JpaRepository<SectionEntity, Integer> {
	List<SectionEntity> findByCardId(int cardId);
}
