package com.hanzo.mochilearn.repository.word;

import com.hanzo.mochilearn.entity.word.WordBookMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordBookMapRepository extends JpaRepository<WordBookMapEntity, Integer> {
    List<WordBookMapEntity> findAllByBookId(Integer bookId);

    @Query(value = "select * from word_book_map where book_id = :bookId and word_id = :wordId", nativeQuery = true)
    WordBookMapEntity findByBookIdAndWordId(@Param("bookId") Integer bookId,@Param("wordId") Integer wordId);

    Integer countByBookId(Integer bookId);
}
