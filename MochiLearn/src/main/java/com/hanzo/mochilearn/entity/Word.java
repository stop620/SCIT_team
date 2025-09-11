package com.hanzo.mochilearn.entity;

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

    @Column(name = "word", nullable = false, length = 50)
    private String word;

    @Column(name = "meaning", nullable = false, length = 50)
    private String meaning;

    @Column(name = "pos", nullable = false, length = 20)
    private String pos;

}