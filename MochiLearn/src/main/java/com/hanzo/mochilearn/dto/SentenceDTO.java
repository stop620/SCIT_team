package com.hanzo.mochilearn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanzo.mochilearn.entity.SentenceEntity;

import com.hanzo.mochilearn.entity.Token;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentenceDTO {

    private int id;
    private int sectionId;
    @JsonProperty("index") //Json과 매핑
    private int sentenceIndex;
    @JsonProperty("time")
    private float time;
    @JsonProperty("japanese")
    private String japanese;
    @JsonProperty("korean")
    private String korean;
    @JsonProperty("japaneseTokens")
    private List<TokenDTO> japaneseTokens;
    
    public static SentenceDTO toDTO(SentenceEntity entity) {
    	
    	SentenceDTO dto = SentenceDTO.builder()
    			.id(entity.getId())
    			.sectionId(entity.getSection().getId())
    			.sentenceIndex(entity.getSentenceIndex())
    			.time(entity.getTime())
    			.japanese(entity.getJapanese())
    			.korean(entity.getKorean())
    			.build();

        List<TokenDTO> tokens = new ArrayList<>();
        for(Token token : entity.getTokens()) {
            tokens.add(TokenDTO.toDTO(token));
        }
    	
    	return dto;
    }
}
