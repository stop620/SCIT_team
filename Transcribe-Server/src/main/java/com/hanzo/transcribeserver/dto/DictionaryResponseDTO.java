package com.hanzo.transcribeserver.dto;


import java.util.List;

public record DictionaryResponseDTO(
        List<String> kanji,
        List<String> kana,
        List<String> partOfSpeech,
        List<String> gloss,
        List<String> examples
) {
}