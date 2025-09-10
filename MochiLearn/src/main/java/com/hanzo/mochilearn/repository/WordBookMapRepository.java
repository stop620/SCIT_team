package com.hanzo.mochilearn.repository;

import com.hanzo.mochilearn.entity.WordBookMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WordBookMapRepository extends JpaRepository<WordBookMapEntity, Integer> {
}
