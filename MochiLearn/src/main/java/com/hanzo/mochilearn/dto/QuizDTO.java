package com.hanzo.mochilearn.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class QuizDTO {

    private List<String> japanese;
    private String korean;
    private int level;
}
