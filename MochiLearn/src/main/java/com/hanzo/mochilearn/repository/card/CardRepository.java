package com.hanzo.mochilearn.repository.card;



import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.card.CardEntity;

import jakarta.transaction.Transactional;


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
    
 // 커스텀 삭제 쿼리 추가
    @Modifying
    @Transactional
    @Query("DELETE FROM CardEntity c WHERE c.id = :cardId")
    int deleteCardByIdCustom(@Param("cardId") Integer cardId);

}
