package com.hanzo.transcribeserver.controller;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.hanzo.transcribeserver.dto.SpeechResponseDTO;
import com.hanzo.transcribeserver.service.SpeechService;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:9000")
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping("/api/speech")
    public SpeechResponseDTO assessPronunciation(@RequestParam("audioFile") MultipartFile file,
                                                   @RequestParam("referenceText") String referenceText) throws Exception {
        log.debug("요청수신");

        SpeechResponseDTO result = speechService.pronunciationAssessment(file, referenceText);

        log.debug("result: {}", result);

        return result;
    }
}
