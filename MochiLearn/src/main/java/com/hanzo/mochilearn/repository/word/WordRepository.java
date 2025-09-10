package com.hanzo.mochilearn.repository.word;

import com.hanzo.mochilearn.entity.word.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WordRepository extends JpaRepository<Word, Integer> {

    boolean existsByWordAndPos(String word, String pos);

    Word findByWordAndPos(String word, String pos);
}
