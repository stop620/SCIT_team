package com.hanzo.mochilearn.dto.word;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanzo.mochilearn.entity.word.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDTO {

    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("contents")
    private String contents;

    private boolean isShare;

    private Integer memberId;

    public static BookDTO toDTO(Book bookEntity) {
        BookDTO bookDTO = new BookDTO();

        bookDTO.setId(bookEntity.getId());
        bookDTO.setTitle(bookEntity.getTitle());
        bookDTO.setContents(bookEntity.getContents());
        bookDTO.setShare(bookEntity.isShare());
        bookDTO.setMemberId(bookEntity.getMember().getId());

        return bookDTO;
    }
}
