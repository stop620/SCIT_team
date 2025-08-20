package com.hanzo.mochilearn.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.StudyEntity;



@Repository
public interface StudyRepository extends JpaRepository<StudyEntity, Long>{
	Page<StudyEntity> findAllByOrderByLikeCountDesc(Pageable pageable);       // 인기순
    Page<StudyEntity> findAllByOrderByCreatedDateDesc(Pageable pageable);     // 최신순
}
