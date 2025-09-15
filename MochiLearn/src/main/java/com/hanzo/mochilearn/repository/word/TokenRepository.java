package com.hanzo.mochilearn.repository.word;

import com.hanzo.mochilearn.entity.word.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
}
