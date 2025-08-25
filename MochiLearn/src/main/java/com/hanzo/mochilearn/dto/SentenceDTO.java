package com.hanzo.mochilearn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
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
}
