package com.hanzo.mochilearn.dto.card;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hanzo.mochilearn.entity.card.CardEntity;
import com.hanzo.mochilearn.entity.card.SectionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {

    private int id;
    private String title;
    private String url;
    private String level;
    private int like;
    private LocalDateTime createdDate;
    private int memberId;
    private String tag;

    private List<SectionDTO> sections;

    // 좋아요 여부 필드 추가 (DB 컬럼 아님, API응답용)
    private Boolean liked;
    
    /**
     * CardEntity -> CardDTO 변환 (좋아요 여부 별도 전달 버전)
     * @param entity 카드 엔티티
     * @param liked 좋아요 여부 (로그인한 사용자의)
     * @return CardDTO
     */
    public static CardDTO toDTO(CardEntity entity, Boolean liked) {
        CardDTO dto = CardDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .url(entity.getUrl())
                .level(entity.getLevel().toString())
                .like(entity.getLike())
                .createdDate(entity.getCreatedDate())
                .memberId(entity.getMemberId())
                .tag(entity.getTag())
                .liked(liked)        // 좋아요 여부 설정
                .build();

        List<SectionDTO> sections = new ArrayList<>();
        for(SectionEntity section : entity.getSections()) {
            SectionDTO sectionDTO = SectionDTO.toDTO(section);
            sections.add(sectionDTO);
        }
        dto.setSections(sections);

        return dto;
    }

    /**
     * 기존 toDTO 오버로드 (liked 정보 없이 호출 시)
     */
    public static CardDTO toDTO(CardEntity entity) {
        return toDTO(entity, false); // 기본값 false
    }
}
