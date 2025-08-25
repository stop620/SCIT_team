package com.hanzo.mochilearn.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {

    private int id;
    private String title;
    private String url;
    private String level;
    private int like;
    private LocalDateTime createdDate;
    private int memberId;
    private String tag;

    private List<SectionDTO> sections;

}
