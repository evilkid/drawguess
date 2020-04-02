package com.wekay.drawguess.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ouerghi Yassine
 */
@Service
@RequiredArgsConstructor
public class WordService {

    private final String API_URL = "https://www.wordgenerator.net/application/p.php?type=1&id=charades_moderate&spaceflag=false";

    private final RestTemplate restTemplate;

    public List<String> generateWords() {

        String words = restTemplate.getForObject(API_URL, String.class);

        if (words == null) {
            return Collections.emptyList();
        }

        List<String> wordList = Arrays.asList(words.split(","));
        Collections.shuffle(wordList);

        return wordList.subList(0, 3);
    }
}
