package com.isums.aiservice.controllers;

import com.isums.aiservice.domains.dtos.ApiResponse;
import com.isums.aiservice.domains.dtos.ApiResponses;
import com.isums.aiservice.domains.dtos.ScoreRequest;
import com.isums.aiservice.domains.dtos.ScoreResponse;
import com.isums.aiservice.infrastructures.abstracts.EifScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scoring")
@RequiredArgsConstructor
public class ScoringController {

    private final EifScoringService eifScoringService;

    @PostMapping("/eif")
    public ApiResponse<ScoreResponse> scoreEif(@RequestBody ScoreRequest req) {
        var res = eifScoringService.score(req);
        return ApiResponses.ok(res, "Score calculation successful");
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
