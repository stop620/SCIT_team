package com.hanzo.mochilearn.service;

import com.hanzo.mochilearn.dto.word.BookDTO;
import com.hanzo.mochilearn.dto.word.WordDTO;
import com.hanzo.mochilearn.entity.word.Book;
import com.hanzo.mochilearn.entity.MemberEntity;
import com.hanzo.mochilearn.entity.word.Word;
import com.hanzo.mochilearn.entity.word.WordBookMapEntity;
import com.hanzo.mochilearn.repository.word.BookRepository;
import com.hanzo.mochilearn.repository.MemberRepository;
import com.hanzo.mochilearn.repository.word.WordBookMapRepository;
import com.hanzo.mochilearn.repository.word.WordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final WordBookMapRepository wordBookMapRepository;
    private final WordRepository wordRepository;

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

    // 단어장의 단어들을 반환하는 메서드
    public List<WordDTO> getWords(Integer bookId) {

        List<WordBookMapEntity> mapEntities = wordBookMapRepository.findAllByBookId(bookId);
        log.debug("[단어장 연결 엔티티]: {}", mapEntities);

        List<Word> wordEntities = mapEntities.stream()
                .map(WordBookMapEntity::getWord).collect(Collectors.toList());
        log.debug("[단어장 {}의 단어들]: {}", bookId, wordEntities);

        List<WordDTO> wordDTOs = new ArrayList<>();
        for (Word entity : wordEntities) {
            WordDTO wordDTO = WordDTO.builder()
                    .id(entity.getId())
                    .selectWord(entity.getSelectWord())
                    .kanji(Arrays.asList(entity.getKanji()))
                    .kana(Arrays.asList(entity.getKana()))
                    .gloss(Arrays.asList(entity.getMeaning()))
                    .partOfSpeech(Arrays.asList(entity.getPos()))
                    .build();

            List<String> examples = new ArrayList<>();
            examples.add(entity.getJpExample());
            examples.add(entity.getKrExample());

            wordDTO.setExamples(examples);

            wordDTOs.add(wordDTO);
        }

        return wordDTOs;
    }


    public void removeBook(Integer bookId, Integer memberId) {

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(()->new EntityNotFoundException("멤버 없음"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("단어장 없음"));

        if(book.getMember().getId() == member.getId()) {
            bookRepository.delete(book);

            log.debug("[단어장 삭제 완료]: {}", book);
        }
    }
}
