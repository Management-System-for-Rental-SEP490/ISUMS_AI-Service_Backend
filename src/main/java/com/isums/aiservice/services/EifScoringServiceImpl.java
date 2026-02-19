package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.ModelBundle;
import com.isums.aiservice.domains.dtos.ScoreRequest;
import com.isums.aiservice.domains.dtos.ScoreResponse;
import com.isums.aiservice.infrastructures.abstracts.EifScoringService;
import com.isums.aiservice.infrastructures.abstracts.ModelStoreService;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.AbstractPrediction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EifScoringServiceImpl implements EifScoringService {

    private final ModelStoreService modelStoreService;

    @Override
    public ScoreResponse score(ScoreRequest req) {

        ModelBundle bundle = modelStoreService.getModelBundle(req.houseId(), req.areaId());

        RowData row = new RowData();
        Map<String, Double> features = req.features();

        for (String feats : bundle.orderedFeatures()) {
            Double value = features.get(feats);
            row.put(feats, value != null ? Double.toString(value) : "0");
        }

        double score = predictAnomalyScore(bundle, row);
        boolean alert = score > bundle.threshold();
        return new ScoreResponse(true, bundle.modelId(), bundle.version(), score, bundle.threshold(), alert);
    }

    private static double predictAnomalyScore(ModelBundle bundle, RowData row) {
        try {

            double offset = 0.0;

            double[] raw = bundle.model().predictRaw(row, offset);

            if (raw.length > 0) {
                return raw[0];
            }

            return offset;
        } catch (Exception e) {
            throw new RuntimeException("MOJO predict failed: " + e.getMessage(), e);
        }
    }
}
