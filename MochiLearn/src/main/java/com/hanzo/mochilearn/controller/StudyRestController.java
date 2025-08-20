package com.hanzo.mochilearn.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hanzo.mochilearn.dto.StudyDTO;
import com.hanzo.mochilearn.service.StudyService;

import lombok.RequiredArgsConstructor;
@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/study")
public class StudyRestController {
    private final StudyService studyService;

    @GetMapping("/cards")
    public List<StudyDTO> getCards(
            @RequestParam(name="sort", defaultValue = "popular") String sort,
            @RequestParam(name="page", defaultValue = "0") int page,
            @RequestParam(name="size", defaultValue = "12") int size) {
        return studyService.getPagedCards(sort, page, size);
    }
}
