package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.QuizAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswerEntity, Integer> {
}
