package com.isums.aiservice.domains.dtos;

import java.util.Map;

public record TranslateTextResponse(
        Map<String, String> translations,
        Map<String, String> statuses,
        String provider,
        Map<String, String> errors
) {
}
