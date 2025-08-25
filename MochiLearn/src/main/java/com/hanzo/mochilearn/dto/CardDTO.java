package com.hanzo.mochilearn.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
