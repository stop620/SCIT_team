package com.hanzo.mochilearn.dto;

import com.hanzo.mochilearn.dto.card.SentenceDTO;
import com.hanzo.mochilearn.dto.quiz.QuizDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TranscriptResponseDTO { // 요청 응답 dto

    private List<SentenceDTO> sentences;
    private int level;
    private List<QuizDTO> quizSentences;
}
