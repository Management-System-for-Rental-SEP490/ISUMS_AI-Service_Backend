package com.isums.aiservice.domains.dtos;

public record TextTranslationResult(
        String sourceLanguage,
        String targetLanguage,
        String translatedText,
        String provider,
        String status
) {
}
