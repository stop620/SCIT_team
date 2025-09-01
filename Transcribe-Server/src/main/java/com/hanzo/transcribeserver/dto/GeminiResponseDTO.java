package com.hanzo.transcribeserver.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeminiResponseDTO {

    @SerializedName("transcriptions")
    private List<SentenceDTO> sentences;

    private int level;
    @SerializedName("quiz_sentences")
    private List<QuizDTO> quizSentences;
}
