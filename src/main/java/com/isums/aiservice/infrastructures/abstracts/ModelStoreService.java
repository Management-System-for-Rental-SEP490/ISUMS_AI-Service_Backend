package com.isums.aiservice.infrastructures.abstracts;

import com.isums.aiservice.domains.dtos.ModelBundle;

import java.util.UUID;

public interface ModelStoreService {
    public ModelBundle getModelBundle(UUID houseId, UUID areaId);
    public void invalidateCache(UUID houseId, UUID areaId);
}
