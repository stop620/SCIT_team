package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.MemberQuizHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberQuizHistoryRepository extends JpaRepository<MemberQuizHistoryEntity, Integer> {
    List<MemberQuizHistoryEntity> findByMemberId(int memberId);
}
