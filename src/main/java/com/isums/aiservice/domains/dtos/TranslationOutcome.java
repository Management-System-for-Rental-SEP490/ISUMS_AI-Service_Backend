package com.isums.aiservice.domains.dtos;

public record TranslationOutcome(
        String sourceLanguage,
        String targetLanguage,
        String translatedText,
        String provider,
        String status,
        String errorMessage
) {
    public boolean isDone() {
        return "DONE".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}
