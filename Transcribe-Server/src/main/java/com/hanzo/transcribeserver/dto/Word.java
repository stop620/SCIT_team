package com.hanzo.transcribeserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Word {
    private String word;
    private String errorType;
    private double accuracyScore;
}
