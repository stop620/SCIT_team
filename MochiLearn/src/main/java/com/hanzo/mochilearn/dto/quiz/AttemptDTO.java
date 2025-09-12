package com.hanzo.mochilearn.dto.quiz;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptDTO {

    private String attemptId;
    private String sessionId;
    private String attemptNum;
    private String score;

    private LocalDateTime attemptDate;
}
