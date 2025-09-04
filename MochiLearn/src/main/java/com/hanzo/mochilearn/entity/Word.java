package com.hanzo.mochilearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "word")
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id", nullable = false)
    private Integer id;

    @Column(name = "word", nullable = false, length = 50)
    private String word;

    @Column(name = "meaning", nullable = false, length = 50)
    private String meaning;

    @Column(name = "description")
    private String description;

    @Column(name = "word_voice")
    private String wordVoice;

}