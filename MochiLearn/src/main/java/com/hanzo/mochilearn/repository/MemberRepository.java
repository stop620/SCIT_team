package com.hanzo.mochilearn.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.MemberEntity;


@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Integer> {
	List<MemberEntity> findByNicknameContainingIgnoreCase(String nickname);
}
