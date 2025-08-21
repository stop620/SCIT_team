package com.hanzo.mochilearn.dto;

import java.time.LocalDateTime;

import com.hanzo.mochilearn.entity.StudyEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyDTO {
    private Long cardId;
    private String url;
    private String title;
    private Integer level;
    private String genre;
    private LocalDateTime createdDate;
    private Integer like;
    private Long memberId; // member_id 필드. 필요에 따라 MemberDTO로도 변환 가능
    
    public static StudyDTO fromEntity(StudyEntity entity) {
        return StudyDTO.builder()
                .cardId(entity.getCardId())
                .url(entity.getUrl())
                .title(entity.getTitle())
                .level(entity.getLevel())
                .genre(entity.getGenre())
                .createdDate(entity.getCreatedDate())
                .like(entity.getLike())
                // 멤버 정보가 있으면 .memberId(entity.getMember().getMemberId()) 등으로 작성
                .build();
    }
}
