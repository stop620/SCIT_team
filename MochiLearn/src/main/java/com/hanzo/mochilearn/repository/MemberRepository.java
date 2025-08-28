package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Integer> {
}
