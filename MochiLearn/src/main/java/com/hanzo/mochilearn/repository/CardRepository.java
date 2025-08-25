package com.hanzo.mochilearn.repository;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.CardEntity;



@Repository
public interface CardRepository extends JpaRepository<CardEntity, Integer> {
    // 최신순 전체 조회
    Page<CardEntity> findAllByOrderByCreatedDateDesc(Pageable pageable);

    // 인기순 전체 조회
    Page<CardEntity> findAllByOrderByLikeDesc(Pageable pageable);

    // 제목 포함 + 최신순 정렬
    Page<CardEntity> findByTitleContainingIgnoreCaseOrderByCreatedDateDesc(String title, Pageable pageable);

    // 제목 포함 + 인기순 정렬
    Page<CardEntity> findByTitleContainingIgnoreCaseOrderByLikeDesc(String title, Pageable pageable);



}

