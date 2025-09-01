package com.hanzo.transcribeserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SentenceDTO { // 요청 응답 dto

    private int index;
    private float time;
    private String japanese;
    private String korean;
}