package com.hanzo.transcribeserver.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GeminiQuizDTO {

    private String japanese;
    private String korean;
    private int level;
}
