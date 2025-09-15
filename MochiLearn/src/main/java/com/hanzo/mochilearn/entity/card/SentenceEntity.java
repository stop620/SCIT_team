package com.hanzo.mochilearn.entity.card;


import com.hanzo.mochilearn.entity.word.Token;
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

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String japanese;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String korean;

    // --- 연관관계 매핑 ---
    // Sentence(1) : Token(N) 관계
    // mappedBy = "sentence": 연관관계의 주인이 Token 엔티티의 "sentence" 필드임을 명시
    // CascadeType.ALL: Sentence가 저장/삭제될 때 연관된 Token들도 함께 저장/삭제
    // orphanRemoval = true: Sentence의 tokens 리스트에서 Token이 제거되면 DB에서도 삭제
    @OneToMany(mappedBy = "sentence", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Token> tokens = new ArrayList<>();

    // --- 연관관계 편의 메소드 ---
    // Sentence에 Token을 추가할 때, Token에도 Sentence를 설정해주는 편의 메소드
    public void addToken(Token token) {
        this.tokens.add(token);
        token.setSentence(this);
    }
}
