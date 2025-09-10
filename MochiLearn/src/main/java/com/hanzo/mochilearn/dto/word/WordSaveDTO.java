package com.hanzo.mochilearn.dto.word;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordSaveDTO {

    private String word;
    private String meaning;
    private String pos;

    private Integer bookId;
}
