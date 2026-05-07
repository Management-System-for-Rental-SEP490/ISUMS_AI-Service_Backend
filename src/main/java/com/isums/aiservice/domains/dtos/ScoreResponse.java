package com.isums.aiservice.domains.dtos;

public record ScoreResponse(
        boolean ok,
        String modelId,
        String version,
        double score,
        double threshold,
        boolean alert,
        String skipReason,
        String stream
) {
    public static ScoreResponse ok(String modelId, String version,
                                   double score, double threshold,
                                   boolean alert, String stream) {
        return new ScoreResponse(true, modelId, version,
                score, threshold, alert, null, stream);
    }

    public static ScoreResponse skip(String reason, String stream) {
        return new ScoreResponse(false, null, null,
                0.0, 0.0, false, reason, stream);
    }

    public static ScoreResponse error(String reason, String stream) {
        return new ScoreResponse(false, null, null,
                0.0, 0.0, false, "error:" + reason, stream);
    }
}
