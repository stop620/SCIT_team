package com.hanzo.mochilearn.entity.word;

import com.hanzo.mochilearn.entity.card.SentenceEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "token")
@EntityListeners(AuditingEntityListener.class)
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer tokenId;

    @Column(name = "token_index", nullable = false)
    private Integer tokenIndex;

    @Column(name = "surface", nullable = false)
    private String surface;

    @Column(name = "part_of_speech", nullable = false)
    private String partOfSpeech;

    @Column(name = "base_form", nullable = false)
    private String baseForm;

    @Column(name = "reading")
    private String reading;

    // --- 연관관계 매핑 ---
    // Token(N) : Sentence(1) 관계
    // FetchType.LAZY: Token 조회 시 바로 Sentence를 로딩하지 않음 (성능 최적화)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false) // FK 컬럼명 지정
    private SentenceEntity sentence;
}
