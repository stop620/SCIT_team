package com.hanzo.mochilearn.dto;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.SectionEntity;

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
    
    
    public static CardDTO toDTO(CardEntity entity) {
    	
    	CardDTO dto = CardDTO.builder()
    			.id(entity.getId())
    			.title(entity.getTitle())
    			.url(entity.getUrl())
    			.level(entity.getLevel().toString())
    			.like(entity.getLike())
    			.createdDate(entity.getCreatedDate())
    			.memberId(entity.getMemberId())
    			.tag(entity.getTag())
    			.build();
    	
    	List<SectionDTO> sections = new ArrayList<>();
    	
    	for(SectionEntity section : entity.getSections()) {
    		SectionDTO sectionDTO = SectionDTO.toDTO(section);
    		
    		sections.add(sectionDTO);
    	}
    	
    	dto.setSections(sections);
    	
    	return dto;
    }
    
}
