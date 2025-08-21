package com.hanzo.mochilearn.entity;



import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "level")
    private Integer level;

    @Column(name = "genre", length = 50)
    private String genre;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "like") // 예약어이지만 보통 JPA에서는 가능. 문제가 있으면 like_count 등으로 컬럼명 변경 추천
    private Integer like;

    // 외래키가 필요하면 아래 추가(주석 해제 + Member 엔티티 필요)
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "FK_card_member"))
    // private Member member;
}
