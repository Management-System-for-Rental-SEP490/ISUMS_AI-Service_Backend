package com.isums.aiservice.cores;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ModelKeyResolver {

    public static String lastestKey(String prefix, UUID houseId, UUID areaId) {
        if (areaId != null) {
            return "%s/house_%s/area_%s/latest.json".formatted(prefix, houseId, areaId);
        }

        return "%s/house_%s/latest.json".formatted(prefix, houseId);
    }

    public static String metaKey(String prefix, UUID houseId, UUID areaId, String version) {
        if (areaId != null) {
            return "%s/house_%s/area_%s/%s/meta.json".formatted(prefix, houseId, areaId, version);
        }
        return "%s/house_%s/%s/meta.json".formatted(prefix, houseId, version);
    }

    public static String mojoKey(String prefix, UUID houseId, UUID areaId, String version) {
        if (areaId != null) {
            return "%s/house_%s/area_%s/%s/model.mojo".formatted(prefix, houseId, areaId, version);
        }
        return "%s/house_%s/%s/model.mojo".formatted(prefix, houseId, version);
    }
}
