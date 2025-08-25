package com.hanzo.mochilearn.entity;



import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

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
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Integer cardId;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "level")
    private Integer level;

    @CreatedDate
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "like")
    private Integer like;
    
    @Column(name = "tag", length = 255)
    private String tag;

    // 외래키가 필요하면 아래 추가(주석 해제 + Member 엔티티 필요)
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "FK_card_member"))
    // private Member member;
}
