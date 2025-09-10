package com.hanzo.mochilearn.repository.quiz;

import com.hanzo.mochilearn.entity.quiz.QuizAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswerEntity, Integer> {
}
