package com.hanzo.mochilearn.repository.word;

import com.hanzo.mochilearn.entity.word.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    List<Book> findAllByMemberId(Integer memberId);
}
