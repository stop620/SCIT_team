package com.hanzo.transcribeserver.dict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sense {
    // 품사 (partOfSpeech)
    @JsonProperty("partOfSpeech")
    private List<String> partOfSpeech;

    // 뜻 (gloss)
    @JsonProperty("gloss")
    private List<Gloss> gloss;

    // 예문 (examples)
    @JsonProperty("examples")
    private List<Example> examples;
}
