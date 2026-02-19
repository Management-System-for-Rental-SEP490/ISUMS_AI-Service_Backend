package com.isums.aiservice.domains.dtos;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;

import java.util.List;

public record ModelBundle(
        String modelId,
        String version,
        double threshold,
        List<String> orderedFeatures,
        EasyPredictModelWrapper model
) {}
