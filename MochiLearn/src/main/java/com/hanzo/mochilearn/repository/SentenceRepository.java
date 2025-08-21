package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.SentenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentenceRepository extends JpaRepository<SentenceEntity, Integer> {
}
