package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.ModelBundle;
import com.isums.aiservice.domains.dtos.ScoreRequest;
import com.isums.aiservice.domains.dtos.ScoreResponse;
import com.isums.aiservice.infrastructures.abstracts.EifScoringService;
import com.isums.aiservice.infrastructures.abstracts.ModelStoreService;
import hex.genmodel.easy.RowData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EifScoringServiceImpl implements EifScoringService {

    private final ModelStoreService modelStoreService;

    @Override
    public ScoreResponse score(ScoreRequest req) {
        ModelBundle bundle = modelStoreService.getModelBundle(
                req.houseId(), req.areaId(), req.stream());

        if (bundle == null) {
            log.warn("no_model: house={} area={} stream={}",
                    req.houseId(), req.areaId(), req.stream());
            return ScoreResponse.skip("no_model", req.stream());
        }

        try {
            RowData row = new RowData();
            for (String feat : bundle.orderedFeatures()) {
                Double value = req.features().get(feat);
                if (value == null || value.isNaN() || value.isInfinite()) {
                    row.put(feat, "0.0");
                } else {
                    row.put(feat, Double.toString(value));
                }
            }

            double score = predictAnomalyScore(bundle, row);
            boolean alert = score > bundle.threshold();

            log.info("score_ok: thing={} stream={} score={} threshold={} alert={}",
                    req.thing(), req.stream(), score, bundle.threshold(), alert);

            return ScoreResponse.ok(
                    bundle.modelId(), bundle.version(),
                    score, bundle.threshold(), alert, req.stream()
            );

        } catch (Exception e) {
            log.error("score_error: thing={} stream={} err={}",
                    req.thing(), req.stream(), e.getMessage());
            return ScoreResponse.error(e.getMessage(), req.stream());
        }
    }

    private static double predictAnomalyScore(ModelBundle bundle, RowData row) {
        try {
            var prediction = bundle.model().predictAnomalyDetection(row);
            return prediction.normalizedScore;

        } catch (Exception e) {
            throw new RuntimeException("MOJO predict failed: " + e.getMessage(), e);
        }
    }
}
