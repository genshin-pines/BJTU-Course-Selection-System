package com.bjtu.review.utils;

import cn.hutool.dfa.WordTree;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SensitiveWordFilter {

    @Value("${sensitive-words}")
    private String sensitiveWordsConfig;

    private WordTree wordTree;

    @PostConstruct
    public void init() {
        List<String> sensitiveWords = Arrays.asList(sensitiveWordsConfig.split(","));
        wordTree = new WordTree();
        wordTree.addWords(sensitiveWords);
    }

    public boolean containsSensitiveWord(String text) {
        return wordTree.isMatch(text);
    }
}
