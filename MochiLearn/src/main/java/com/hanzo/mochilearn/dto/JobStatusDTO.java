package com.hanzo.mochilearn.dto;

import lombok.Data;
import java.util.List;

/** 작업의 상태와 결과(또는 에러)를 담는 DTO */
@Data
public class JobStatusDTO {

    private String status; // "PROCESSING", "COMPLETED", "FAILED"

    private List<TranscriptResponseDTO> result;
    private String error;

    public JobStatusDTO(String status) {
        this.status = status;
    }
}