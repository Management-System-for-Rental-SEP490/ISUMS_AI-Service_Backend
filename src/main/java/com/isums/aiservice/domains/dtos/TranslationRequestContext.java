package com.isums.aiservice.domains.dtos;

public record TranslationRequestContext(
        String resourceType,
        String translationIntent,
        boolean customerFacing,
        String sourceLanguage,
        String targetLanguage
) {
}
