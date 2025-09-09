package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.BookDTO;
import com.hanzo.mochilearn.entity.Book;
import com.hanzo.mochilearn.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookService {

    private final BookRepository bookRepository;

    public List<BookDTO> getBooks(int memberId) {

        List<BookDTO> books = new ArrayList<>();
        List<Book> bookEntities = bookRepository.findAllByMemberId(memberId);

        for (Book entity : bookEntities) {
            BookDTO bookDTO = BookDTO.builder()
                    .id(entity.getId())
                    .title(entity.getTitle())
                    .contents(entity.getContents())
                    .isShare(entity.getIsShare())
                    .memberId(memberId)
                    .build();

            books.add(bookDTO);
        }
        log.debug("[wordbook] memberId: {}, books: {}", memberId, books);
        return books;
    }
}
