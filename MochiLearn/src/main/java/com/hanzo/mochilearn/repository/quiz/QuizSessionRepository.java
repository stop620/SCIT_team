package com.hanzo.mochilearn.repository.quiz;

import com.hanzo.mochilearn.entity.quiz.QuizSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSessionEntity, Integer> {

    @Query(value = """
    SELECT DISTINCT qa.quiz_id
    FROM quiz_answer qa
    JOIN quiz_attempt a ON qa.attempt_id = a.attempt_id
    JOIN quiz_session s ON a.session_id = s.session_id
    WHERE s.member_id = :memberId
      AND s.level = :level
      AND qa.is_correct = TRUE
""", nativeQuery = true)
    List<Integer> FindAllIdByMemberId(@Param("level") int level, @Param("memberId") int memberId);
}
