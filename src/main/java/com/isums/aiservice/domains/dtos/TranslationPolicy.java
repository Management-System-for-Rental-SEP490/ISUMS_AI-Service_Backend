package com.isums.aiservice.domains.dtos;

import java.util.List;

public record TranslationPolicy(
        boolean customerFacing,
        boolean formalRequested,
        boolean bedrockPolishEnabled,
        List<String> terminologyNames
) {
}
