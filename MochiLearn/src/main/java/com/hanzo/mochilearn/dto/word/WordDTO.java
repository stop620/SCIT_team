package com.hanzo.mochilearn.dto.word;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class WordDTO {
    private Integer id;
    private Integer bookId;
    private String selectWord;
    private List<String> kanji;
    private List<String> kana;
    private List<String> partOfSpeech;
    private List<String> gloss;
    private List<String> examples;
}

