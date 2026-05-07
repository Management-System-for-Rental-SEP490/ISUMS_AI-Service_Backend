package com.isums.aiservice.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TranslateTextRequest(
        @NotBlank
        @Size(max = 10_000)
        String text,

        @Size(min = 2, max = 5)
        String sourceLanguage,

        @NotEmpty
        @Size(min = 1, max = 5)
        List<@NotBlank String> targetLanguages,

        String intent,

        String resourceType,

        Boolean customerFacing
) {
}
