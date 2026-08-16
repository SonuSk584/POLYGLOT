package com.polyglot.chat.service;

import java.util.Map;
import java.util.Set;

public interface TranslationService {
    
    /**
     * Translates text from source language to multiple target languages
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language code (ISO 639-1)
     * @param targetLanguages Set of target language codes (ISO 639-1)
     * @return Map of language code to translated text
     */
    Map<String, String> translateText(String text, String sourceLanguage, Set<String> targetLanguages);
}