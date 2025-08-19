package com.hanzo.mochilearn.dto;

import lombok.Data;

@Data
public class TranscriptRequestDTO {
    private String url;
    private float start;
    private float end;
}
