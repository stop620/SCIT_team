package com.hanzo.mochilearn.dto;

import java.time.LocalDateTime;

import com.hanzo.mochilearn.entity.CardEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDTO {
    private Integer cardId;
    private String url;
    private String title;
    private Integer level;
    private LocalDateTime createdDate;
    private Integer like;
    private String tag;
    private Integer memberId; // member_id 필드. 필요에 따라 MemberDTO로도 변환 가능
    
    public static CardDTO fromEntity(CardEntity entity) {
        return CardDTO.builder()
                .cardId(entity.getCardId())
                .url(entity.getUrl())
                .title(entity.getTitle())
                .level(entity.getLevel())
                .createdDate(entity.getCreatedDate())
                .like(entity.getLike())
                .tag(entity.getTag())
                // 멤버 정보가 있으면 .memberId(entity.getMember().getMemberId()) 등으로 작성
                .build();
    }
}
