package com.hanzo.mochilearn.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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
}
