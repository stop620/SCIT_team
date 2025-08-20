package com.hanzo.mochilearn.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyDTO {
	private Long cardId;		//학습카드 고유 ID
    private String japanese;	//일본어 내용
    private String korean;		//한국어 해석
    private String url;			//관련 미디어 URL
    private Integer timeline;	//미디어 타임라인 (초)
    private Integer level;		//난이도
    private String genre;		//장르
    private Integer likeCount;	//좋아요
    private LocalDateTime createdDate;  // 작성일
}
