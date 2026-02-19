package com.isums.aiservice.infrastructures.abstracts;

import com.isums.aiservice.domains.dtos.ScoreRequest;
import com.isums.aiservice.domains.dtos.ScoreResponse;

public interface EifScoringService {

    public ScoreResponse score(ScoreRequest req);
}
