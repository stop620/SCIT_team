package com.hanzo.transcribeserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenDTO {

    private int index;
    private String surface;
    private String base;
    private String pos;
    private String reading;
}
