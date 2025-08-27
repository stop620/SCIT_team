package com.hanzo.mochilearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDTO {

    private int id;
    private List<String> japanese;
    private String korean;
    private int level;
}
