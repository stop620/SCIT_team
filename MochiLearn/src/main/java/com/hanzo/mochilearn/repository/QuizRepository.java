package com.hanzo.mochilearn.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hanzo.mochilearn.entity.QuizEntity;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {

    @Query(value = "SELECT * FROM quiz WHERE level = :level ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findRandomQuizByLevel(int level, int limit);

    @Query(value = "SELECT * FROM quiz WHERE level = :level AND quiz_id NOT IN :solvedIds ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findUnsolvedRandomQuizByLevel(int level, int limit, List<Integer> solvedIds);

    @Query(value = "SELECT japanese FROM quiz ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<String> findRandomQuiz();
}
