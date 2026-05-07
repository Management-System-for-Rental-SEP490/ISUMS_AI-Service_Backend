package com.isums.aiservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record IssueTextTranslationRequestedEvent(
        UUID requestId,
        String resourceType,
        UUID resourceId,
        String text,
        String sourceLanguage,
        String targetLanguage,
        String translationIntent,
        Boolean customerFacing,
        Instant requestedAt
) {
}
