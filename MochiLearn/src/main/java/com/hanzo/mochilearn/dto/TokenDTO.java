package com.hanzo.mochilearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {

    private int index;
    private String surface;
    private String base;
    private String pos;
    private String reading;
}
