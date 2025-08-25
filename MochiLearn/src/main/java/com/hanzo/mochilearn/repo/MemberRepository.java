package com.hanzo.mochilearn.repo;

import com.hanzo.mochilearn.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 회원 정보 Repository
 */

@Repository
public interface MemberRepository 
	extends JpaRepository<MemberEntity, Integer> {

	Optional<MemberEntity> findByUserId(String userId);
}