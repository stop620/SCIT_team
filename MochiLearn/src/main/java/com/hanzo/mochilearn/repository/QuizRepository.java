package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {

    @Query(value = "SELECT * FROM quiz WHERE level = :level ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findRandomQuizByLevel(int level, int limit);

    @Query(value = "SELECT * FROM quiz WHERE level = :level AND id NOT IN :solvedIds ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findUnsolvedRandomQuizByLevel(int level, int limit, List<Integer> quizIds);


}
