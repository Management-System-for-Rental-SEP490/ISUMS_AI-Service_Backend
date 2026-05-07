package com.isums.aiservice.infrastructures.abstracts;

import com.isums.aiservice.domains.dtos.ModelBundle;

public interface ModelStoreService {
    ModelBundle getModelBundle(String houseId, String areaId, String stream);
    void invalidateCache(String houseId, String areaId);
}
