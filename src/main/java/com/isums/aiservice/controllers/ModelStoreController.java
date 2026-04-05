package com.isums.aiservice.controllers;

import com.isums.aiservice.domains.dtos.ApiResponse;
import com.isums.aiservice.domains.dtos.ApiResponses;
import com.isums.aiservice.infrastructures.abstracts.ModelStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/model-store")
@RequiredArgsConstructor
public class ModelStoreController {

    private final ModelStoreService modelStoreService;

    @PostMapping("/invalidate")
    public ApiResponse<Void> invalidate(@RequestParam String houseId, @RequestParam(required = false) String areaId) {
        modelStoreService.invalidateCache(houseId, areaId);
        return ApiResponses.ok(null, "Cache invalidated");
    }
}