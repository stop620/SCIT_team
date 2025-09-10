package com.hanzo.mochilearn.dto.word;

import com.hanzo.mochilearn.entity.word.Word;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordDTO {

    private Integer id;

    private String word;
    private String meaning;
    private String pos;

    public WordDTO toDTO(Word word) {
        WordDTO dto = new WordDTO();

        dto.setId(word.getId());
        dto.setWord(word.getWord());
        dto.setMeaning(word.getMeaning());
        dto.setPos(word.getPos());

        return dto;
    }
}
