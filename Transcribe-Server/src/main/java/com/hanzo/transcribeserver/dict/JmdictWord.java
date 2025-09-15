package com.hanzo.transcribeserver.dict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JmdictWord {
    // 단어
    @JsonProperty("kanji")
    private List<Kanji> kanji;

    // 읽기(히라가나)
    @JsonProperty("kana")
    private List<Kana> kana;

    // 뜻, 예문
    private Sense sense;

    @JsonProperty("sense")
    private void setSenseFromList(List<Sense> senses) {

        if (senses != null && !senses.isEmpty()) {
            this.sense = senses.get(0);
        }
    }
}