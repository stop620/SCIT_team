package com.hanzo.mochilearn.repository;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.CardEntity;

import java.util.List;


@Repository
public interface CardRepository extends JpaRepository<CardEntity, Integer>, JpaSpecificationExecutor<CardEntity> {

    // 최신순 전체 조회
    Page<CardEntity> findAllByOrderByCreatedDateDesc(Pageable pageable);

    // 인기순 전체 조회
    Page<CardEntity> findAllByOrderByLikeDesc(Pageable pageable);

    Page<CardEntity> findByTitleContainingIgnoreCaseOrMemberIdInOrderByCreatedDateDesc(
            String title, List<Integer> memberIds, Pageable pageable);

    Page<CardEntity> findByTitleContainingIgnoreCaseOrMemberIdInOrderByLikeDesc(
    	    String title, List<Integer> memberIds, Pageable pageable);

    List<CardEntity> findAllByMemberId(Integer memberId);
}
