package com.hanzo.mochilearn.repository.word;

import com.hanzo.mochilearn.entity.word.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Integer> {

    Optional<Word> findBySelectWordAndMeaningAndPos(String word, String meaning, String pos);
}
