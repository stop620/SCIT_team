package com.hanzo.mochilearn.entity.card;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
