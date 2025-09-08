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
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "likes")
public class LikeEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer likeId;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @Column(name = "card_id", nullable = false)
    private Integer cardId;

    @Column(name = "like_time", nullable = false, updatable = false)
    private LocalDateTime likeTime = LocalDateTime.now();

    // 기본 생성자, getter/setter 생략

}
