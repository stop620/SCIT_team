package com.hanzo.mochilearn.repository.quiz;

import com.hanzo.mochilearn.entity.quiz.QuizAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswerEntity, Integer> {
    List<QuizAnswerEntity> findAllByAttempt_AttemptId(Integer attemptId);
}
