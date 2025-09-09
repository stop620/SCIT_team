package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.BookDTO;
import com.hanzo.mochilearn.entity.Book;
import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.repository.BookRepository;
import com.hanzo.mochilearn.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public List<BookDTO> getBooks(int memberId) {

        List<BookDTO> books = new ArrayList<>();
        List<Book> bookEntities = bookRepository.findAllByMemberId(memberId);

        for (Book entity : bookEntities) {
            BookDTO bookDTO = BookDTO.builder()
                    .id(entity.getId())
                    .title(entity.getTitle())
                    .contents(entity.getContents())
                    .memberId(memberId)
                    .build();

            books.add(bookDTO);
        }
        log.debug("[wordbook] memberId: {}, books: {}", memberId, books);
        return books;
    }

    public void createWordBook(BookDTO bookDTO, Integer memberId) throws EntityNotFoundException {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("멤버 정보 없음"));

        try {
            Book bookEntity = Book.builder()
                    .title(bookDTO.getTitle())
                    .contents(bookDTO.getContents())
                    .member(member)
                    .build();

            bookRepository.save(bookEntity);
            log.debug("[단어장 생성 성공]: {}", bookEntity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BookDTO getBook(Integer bookId) {

        Book bookEntity = bookRepository.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("단어장 없음"));

        return BookDTO.toDTO(bookEntity);
    }
}
