package com.bjtu.review.utils;

import cn.hutool.dfa.WordTree;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SensitiveWordFilter {

    private static final Pattern WORD_SPLIT = Pattern.compile("[|&，,、\\s]+");

    private final ResourceLoader resourceLoader;

    @Value("${sensitive-words.file:classpath:sensitive-words.txt}")
    private String sensitiveWordsFile;

    private WordTree wordTree;

    public SensitiveWordFilter(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        initFromWords(loadWordsFromFile());
    }

    void initFromWords(List<String> words) {
        wordTree = new WordTree();
        wordTree.addWords(words);
    }

    public boolean containsSensitiveWord(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return wordTree.isMatch(text);
    }

    public boolean containsSensitiveWord(String... texts) {
        if (texts == null) {
            return false;
        }
        for (String text : texts) {
            if (containsSensitiveWord(text)) {
                return true;
            }
        }
        return false;
    }

    private List<String> loadWordsFromFile() {
        Resource resource = resourceLoader.getResource(sensitiveWordsFile);
        if (!resource.exists()) {
            throw new IllegalStateException("敏感词文件不存在: " + sensitiveWordsFile);
        }
        Set<String> words = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.addAll(parseLine(line));
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取敏感词文件失败: " + sensitiveWordsFile, e);
        }
        if (words.isEmpty()) {
            throw new IllegalStateException("敏感词文件为空: " + sensitiveWordsFile);
        }
        return new ArrayList<>(words);
    }

    private List<String> parseLine(String line) {
        List<String> words = new ArrayList<>();
        if (!StringUtils.hasText(line)) {
            return words;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
            return words;
        }
        for (String part : WORD_SPLIT.split(trimmed)) {
            String word = part.trim();
            if (!StringUtils.hasText(word) || word.startsWith("#")) {
                continue;
            }
            if (word.length() < 2) {
                continue;
            }
            words.add(word);
        }
        return words;
    }
}
