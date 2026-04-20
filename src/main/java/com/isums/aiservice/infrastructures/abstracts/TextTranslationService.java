package com.isums.aiservice.infrastructures.abstracts;

import com.isums.aiservice.domains.dtos.TextTranslationResult;
import com.isums.aiservice.domains.dtos.TranslationPolicy;

public interface TextTranslationService {
    TextTranslationResult translate(String text, String sourceLanguage, String targetLanguage, TranslationPolicy policy);
}
