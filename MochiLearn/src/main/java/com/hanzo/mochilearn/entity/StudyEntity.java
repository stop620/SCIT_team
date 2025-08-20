package com.hanzo.mochilearn.entity;



import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card")
public class StudyEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cardId;
	//학습카드 고유 ID

    @Column(nullable = false, length = 255)
    private String japanese;
    //일본어 내용
    
    @Column(nullable = false, length = 255)
    private String korean;
    //한국어 해석
    
    @Column(length = 255)
    private String url;
    //관련 미디어 URL

    private Integer timeline;
    //미디어 타임라인 (초)

    private Integer level;
    //난이도

    @Column(length = 50)
    private String genre;
    //장르

    @CreationTimestamp
    private LocalDateTime createdDate;
    //작성일

    @Column(name = "`like`")
    private Integer likeCount = 0;
    //좋아요

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id")
//    private Member member;
//    //작성자 ID 멤버엔티티 필요

}
