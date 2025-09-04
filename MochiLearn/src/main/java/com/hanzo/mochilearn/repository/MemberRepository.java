package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Integer> {

    Optional<MemberEntity> findByUserId(String id);
}