package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Integer> {
}
