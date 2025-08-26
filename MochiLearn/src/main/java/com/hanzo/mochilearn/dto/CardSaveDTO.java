package com.hanzo.mochilearn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CardSaveDTO {

    @JsonProperty("card")
    private CardDTO cardDTO;
    @JsonProperty("quiz")
    private List<QuizDTO> quizDtoList;
}
