package com.hanzo.mochilearn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanzo.mochilearn.entity.CardEntity;
import lombok.Data;

import java.util.List;

@Data
public class SectionDTO {

    private int id;
    private int cardId;
    @JsonProperty("start_seconds") //Json과 매핑
    private float startSeconds;
    @JsonProperty("end_seconds")
    private float endSeconds;
    @JsonProperty("section_num")
    private int sectionNum;
    private List<SentenceDTO> sentences;
}
