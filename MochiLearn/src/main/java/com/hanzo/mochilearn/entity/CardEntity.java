package com.hanzo.mochilearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "card")
@EntityListeners(AuditingEntityListener.class)
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Integer id;

    @Column(nullable = false)
    private String title;

    private String url;

    private Integer level;

    private String tag; // 장르 및 기타 태그 (쉼표로 구분된 문자열)

    @Column(name = "`like`") // 'like'는예약어이므로 백틱
    private int like = 0;

    @CreatedDate // 엔티티가 생성될 때 자동으로 현재 시간이 기록
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "member_id")
    private int memberId; // Member 엔티티와 직접적인 연관관계 대신 ID만 저장

    // mappedBy: Section 엔티티에 있는 'card' 필드가 이 관계의 주인임을 명시
    // cascade: Card가 저장/삭제될 때 Section도 함께 저장/삭제되도록 설정
    // orphanRemoval: Card에서 Section이 제거되면 DB에서도 삭제되도록 설정
    @ToString.Exclude
    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SectionEntity> sections = new ArrayList<>();

    //== 연관관계 편의 메소드 ==//
    public void addSection(SectionEntity section) {
        sections.add(section);
        section.setCard(this);
    }
}
