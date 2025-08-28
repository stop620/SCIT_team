package com.hanzo.mochilearn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "section")
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Integer id;

    // @JoinColumn: 외래 키(FK) 컬럼을 지정합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private CardEntity card;

    @Column(name = "start_seconds", nullable = false)
    private Float startSeconds;

    @Column(name = "end_seconds", nullable = false)
    private Float endSeconds;

    @Column(name = "section_num", nullable = false)
    private Integer sectionNum;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SentenceEntity> sentences = new ArrayList<>();

    //== 연관관계 편의 메소드 ==//
    public void addSentence(SentenceEntity sentence) {
        sentences.add(sentence);
        sentence.setSection(this);
    }

}
