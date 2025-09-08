package com.hanzo.transcribeserver.dto;

import com.atilika.kuromoji.ipadic.Token;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SentenceDTO { // 요청 응답 dto

    private int index;
    private float time;
    private String japanese;
    private String korean;
    private List<TokenDTO> japaneseTokens;
}