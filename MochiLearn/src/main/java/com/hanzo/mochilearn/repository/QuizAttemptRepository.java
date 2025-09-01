package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Integer> {
}
