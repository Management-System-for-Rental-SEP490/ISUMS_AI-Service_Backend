package com.isums.aiservice.domains.dtos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ScoreRequest(
        String thing,
        String houseId,
        String areaId,
        Long ts,
        String stream,
        Map<String, Double> features
) {
    public ScoreRequest {
        Objects.requireNonNull(thing, "thing required");
        Objects.requireNonNull(houseId, "houseId required");
        Objects.requireNonNull(stream, "stream required");
        if (!stream.equals("power") && !stream.equals("water"))
            throw new IllegalArgumentException("stream must be power|water");
        if (features == null || features.isEmpty())
            throw new IllegalArgumentException("features required");
    }
}