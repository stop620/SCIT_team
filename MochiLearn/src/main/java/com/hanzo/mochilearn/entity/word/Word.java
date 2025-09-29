package com.hanzo.mochilearn.entity.word;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "word")
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id", nullable = false)
    private Integer id;

    @Column(name = "select_word", nullable = false, length = 50)
    private String selectWord;

    @Column(name = "meaning", nullable = false, length = 50)
    private String meaning;

    @Column(name = "pos", nullable = false, length = 20)
    private String pos;

    @Column(name = "kanji", length = 100)
    private String kanji;

    @Column(name = "kana", nullable = false, length = 100)
    private String kana;

    @Column(name = "jp_example")
    private String jpExample;

    @Column(name = "kr_example")
    private String krExample;
}