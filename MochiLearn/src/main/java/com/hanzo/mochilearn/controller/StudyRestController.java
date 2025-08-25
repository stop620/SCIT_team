package com.hanzo.mochilearn.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hanzo.mochilearn.dto.CardDTO;
import com.hanzo.mochilearn.service.CardService;

import lombok.RequiredArgsConstructor;
@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor

public class StudyRestController {
    private final CardService cs;

    @GetMapping("/api/study/load")
    public List<CardDTO> getCards(
            @RequestParam(name="sort", defaultValue = "popular") String sort,
            @RequestParam(name="page", defaultValue = "0") int page,
            @RequestParam(name="size", defaultValue = "12") int size,
    		@RequestParam(name ="search", defaultValue = "") String search) {
            if (search == null || search.isEmpty()) {
                return cs.getPagedCards(sort, page, size);
            } else {
                return cs.searchCards(sort, page, size, search);
            }
        }
    
}
