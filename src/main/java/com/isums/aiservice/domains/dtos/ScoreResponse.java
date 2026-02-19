package com.isums.aiservice.domains.dtos;

public record ScoreResponse(
        boolean ok,
        String modelId,
        String version,
        double score,
        double threshold,
        boolean alert
) {}