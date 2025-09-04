package com.hanzo.mochilearn.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookDTO {

    private Integer id;
    private String title;
    private String contents;
    private boolean isShare;
    private Integer memberId;
}
