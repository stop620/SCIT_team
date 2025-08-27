package com.hanzo.mochilearn.dto;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hanzo.mochilearn.entity.CardEntity;
import com.hanzo.mochilearn.entity.SectionEntity;
import com.hanzo.mochilearn.entity.SentenceEntity;

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
