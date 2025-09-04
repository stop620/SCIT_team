package com.hanzo.transcribeserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SpeechResponseDTO {

    float accuracyScore;
    float completenessScore;
    List<Word> words;
    float fluencyScore;

    String errorMessage;

    public SpeechResponseDTO(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
