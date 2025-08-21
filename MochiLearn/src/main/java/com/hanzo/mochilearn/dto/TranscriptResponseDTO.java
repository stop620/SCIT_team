package com.hanzo.mochilearn.dto;

import lombok.Data;

@Data
public class TranscriptResponseDTO { // 요청 응답 dto

    private int index;
    private float time;
    private String japanese;
    private String korean;
}
