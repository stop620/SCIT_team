package com.hanzo.mochilearn.repository.quiz;

import com.hanzo.mochilearn.entity.quiz.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {

    @Query(value = "SELECT * FROM quiz WHERE level = :level ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findRandomQuizByLevel(@Param("level") int level,@Param("limit") int limit);

    @Query(value = "SELECT * FROM quiz WHERE level = :level AND quiz_id NOT IN :solvedIds ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<QuizEntity> findUnsolvedRandomQuizByLevel(@Param("level") int level, @Param("limit") int limit, @Param("solvedIds") List<Integer> solvedIds);

    @Query(value = "SELECT japanese FROM quiz ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<String> findRandomQuiz();
}
