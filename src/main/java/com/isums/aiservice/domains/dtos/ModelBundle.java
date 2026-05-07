package com.isums.aiservice.domains.dtos;


import hex.genmodel.easy.EasyPredictModelWrapper;

import java.util.List;

public record ModelBundle(
        String modelId,
        String version,
        String stream,              // "power" | "water"
        double threshold,
        List<String> orderedFeatures,
        EasyPredictModelWrapper model
) {
}
