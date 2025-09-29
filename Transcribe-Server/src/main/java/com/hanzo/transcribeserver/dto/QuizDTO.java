package com.hanzo.transcribeserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class QuizDTO {

    private String japanese;
    private String korean;
    private int level;
}
