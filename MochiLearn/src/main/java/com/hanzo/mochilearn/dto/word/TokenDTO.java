package com.hanzo.mochilearn.dto.word;

import com.hanzo.mochilearn.entity.word.Token;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {

    private int id;
    private int index;
    private String surface;
    private String base;
    private String pos;
    private String reading;

    public static TokenDTO toDTO(Token token) {

        TokenDTO dto = TokenDTO.builder()
                .id(token.getTokenId())
                .index(token.getTokenIndex())
                .surface(token.getSurface())
                .base(token.getBaseForm())
                .pos(token.getPartOfSpeech())
                .reading(token.getReading())
                .build();

        return dto;
    }
}
