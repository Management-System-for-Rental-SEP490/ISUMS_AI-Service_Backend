package com.isums.aiservice.domains.dtos;

public record CustomerFacingPolishResult(
        String text,
        boolean usedBedrock
) {
}
