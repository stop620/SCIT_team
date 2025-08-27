package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {

    @Query(value = "SELECT * FROM quiz WHERE level = :level ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findRandomQuizByLevel(int level, int limit);

    @Query(value = "SELECT * FROM quiz WHERE level = :level AND id NOT IN :solvedIds ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findUnsolvedRandomQuizByLevel(int level, int limit, List<Integer> quizIds);

    @Query(value = "SELECT japanese FROM quiz ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<String> findRandomQuiz();
}
