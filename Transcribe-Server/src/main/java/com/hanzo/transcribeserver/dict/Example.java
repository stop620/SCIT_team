package com.hanzo.transcribeserver.dict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Example {
    @JsonProperty("sentences")
    private List<Sentence> sentences;
}
