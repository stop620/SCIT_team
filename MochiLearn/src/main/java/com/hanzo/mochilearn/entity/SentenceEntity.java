package com.hanzo.mochilearn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sentence")
public class SentenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private SectionEntity section;

    @Column(name = "sentence_index", nullable = false)
    private Integer sentenceIndex;

    @Column(nullable = false)
    private Float time;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String japanese;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String korean;

}
