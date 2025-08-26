package com.hanzo.mochilearn.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
public class JobStatusDTO { // 작업의 상태와 결과(또는 에러)를 담는 DTO

    private String status; // "PROCESSING", "COMPLETED", "FAILED"

    private TranscriptResponseDTO result;
    private String error;

    public JobStatusDTO(String status) {
        this.status = status;
    }
}