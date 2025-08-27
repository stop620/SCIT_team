package com.hanzo.mochilearn.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanzo.mochilearn.entity.SectionEntity;
import com.hanzo.mochilearn.entity.SentenceEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {

    private int id;
    private int cardId;
    @JsonProperty("start_seconds") //Json과 매핑
    private float startSeconds;
    @JsonProperty("end_seconds")
    private float endSeconds;
    @JsonProperty("section_num")
    private int sectionNum;
    private List<SentenceDTO> sentences;
    
    public static SectionDTO toDTO(SectionEntity entity) {
    	
    	SectionDTO dto = SectionDTO.builder()
    			.id(entity.getId())
    			.cardId(entity.getCard().getId())
    			.startSeconds(entity.getStartSeconds())
    			.endSeconds(entity.getEndSeconds())
    			.sectionNum(entity.getSectionNum())
    			.build();
    	
    	List<SentenceDTO> sentences = new ArrayList<>();
    	
    	for(SentenceEntity sentence : entity.getSentences()) {
    		SentenceDTO sentenceDTO = SentenceDTO.toDTO(sentence);
    		
    		sentences.add(sentenceDTO);
    	}
    	
    	dto.setSentences(sentences);
    	
    	return dto;
    }
}
