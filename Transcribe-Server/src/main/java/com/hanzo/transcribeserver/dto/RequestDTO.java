package com.hanzo.transcribeserver.dto;

import lombok.Data;

@Data
public class RequestDTO { // 자막 요청 받는 dto
    private String url;
    private float start;
    private float end;
}