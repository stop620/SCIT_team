package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {

    List<QuizEntity> findRandomQuizByLevel(int level, int quizCount);

    List<QuizEntity> findUnsolvedRandomQuizByLevel(int level, int quizCount, List<Integer> quizIds);
}
