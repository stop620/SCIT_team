package com.hanzo.mochilearn.dto;

import lombok.Data;

@Data
public class TranscriptResponseDTO {

    private int index;
    private float time;
    private String japanese;
    private String korean;
}
