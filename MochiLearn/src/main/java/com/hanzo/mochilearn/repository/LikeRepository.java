package com.hanzo.mochilearn.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.LikeEntity;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Integer>{
	Optional<LikeEntity> findByMemberIdAndCardId(Integer memberId, Integer cardId);
    void deleteByMemberIdAndCardId(Integer memberId, Integer cardId);
	boolean existsByMemberIdAndCardId(Integer memberId, Integer cardId);
}
