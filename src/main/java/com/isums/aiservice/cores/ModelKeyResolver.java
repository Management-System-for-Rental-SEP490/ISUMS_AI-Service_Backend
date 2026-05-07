package com.isums.aiservice.cores;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ModelKeyResolver {

    public static String latestKey(String prefix, String houseId, String areaId, String stream) {
        return scopePath(prefix, houseId, areaId)
                + "/" + stream + "/latest.json";
    }

    public static String metaKey(String prefix, String houseId, String areaId, String stream, String version) {
        return scopePath(prefix, houseId, areaId)
                + "/" + stream + "/" + version + "/meta.json";
    }

    public static String mojoKey(String prefix, String houseId, String areaId, String stream, String version) {
        return scopePath(prefix, houseId, areaId)
                + "/" + stream + "/" + version + "/model.mojo";
    }

    private static String scopePath(String prefix, String houseId, String areaId) {
        String scope = (areaId == null || areaId.isBlank())
                ? "house_" + houseId
                : "house_" + houseId + "/area_" + areaId;
        return prefix.stripTrailing() + "/" + scope;
    }
}
