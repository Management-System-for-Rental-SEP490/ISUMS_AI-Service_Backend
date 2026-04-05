package com.isums.aiservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.isums.aiservice.cores.ModelKeyResolver;
import com.isums.aiservice.domains.dtos.ModelBundle;
import com.isums.aiservice.infrastructures.abstracts.ModelStoreService;
import com.isums.aiservice.infrastructures.clients.S3StoreClient;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelStoreServiceImpl implements ModelStoreService {

    private final S3StoreClient s3StoreClient;
    private final ObjectMapper om;

    @Value("${ai.s3.bucket}")
    private String bucket;
    @Value("${ai.s3.prefix}")
    private String prefix;
    @Value("${ai.scoring.defaultThreshold:0.7}")
    private double defaultThreshold;
    @Value("${ai.cache.ttlSeconds:300}")
    private int ttlSeconds;

    private Cache<String, Optional<ModelBundle>> cache;

    @PostConstruct
    void initCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(500)
                .build();
    }

    @Override
    public ModelBundle getModelBundle(String houseId, String areaId, String stream) {
        if (areaId != null && !areaId.isBlank()) {
            String ck = cacheKey(houseId, areaId, stream);
            Optional<ModelBundle> cached = cache.get(ck, _k -> tryLoad(houseId, areaId, stream));
            if (cached != null && cached.isPresent()) {
                return cached.get();
            }
            log.info("model_fallback: area={} → house={} stream={}", areaId, houseId, stream);
        }

        String hk = cacheKey(houseId, null, stream);
        Optional<ModelBundle> houseModel = cache.get(hk,
                _k -> tryLoad(houseId, null, stream));

        if (houseModel != null && houseModel.isPresent()) {
            return houseModel.get();
        }

        return null;
    }

    private Optional<ModelBundle> tryLoad(String houseId, String areaId, String stream) {
        Path tmp = null;
        try {
            String latestKey = ModelKeyResolver.latestKey(prefix, houseId, areaId, stream);
            byte[] latestBytes = s3StoreClient.getBytes(bucket, latestKey);
            if (latestBytes == null || latestBytes.length == 0) {
                log.warn("model_not_found: house={} area={} stream={}", houseId, areaId, stream);
                return Optional.empty();
            }

            JsonNode latest = om.readTree(new String(latestBytes, StandardCharsets.UTF_8));
            String version = latest.path("version").asString(null);
            if (version == null || version.isBlank()) {
                log.warn("latest_missing_version: house={} stream={}", houseId, stream);
                return Optional.empty();
            }

            String metaKey = ModelKeyResolver.metaKey(prefix, houseId, areaId, stream, version);
            JsonNode meta = om.readTree(
                    new String(s3StoreClient.getBytes(bucket, metaKey), StandardCharsets.UTF_8));

            String modelId  = meta.path("modelId").asString("house_" + houseId + "_" + stream + "_" + version);
            double threshold = meta.path("threshold").path("scoreThreshold").asDouble(defaultThreshold);

            List<String> orderedFeatures = new ArrayList<>();
            JsonNode arr = meta.path("features").path("ordered");
            if (arr.isArray()) {
                for (JsonNode node : arr) orderedFeatures.add(node.asString());
            }
            if (orderedFeatures.isEmpty()) {
                log.warn("meta_missing_features: house={} stream={}", houseId, stream);
                return Optional.empty();
            }

            String mojoKey = ModelKeyResolver.mojoKey(prefix, houseId, areaId, stream, version);
            byte[] mojoBytes = s3StoreClient.getBytes(bucket, mojoKey);

            tmp = Files.createTempFile("eif_", ".mojo");
            Files.write(tmp, mojoBytes);
            MojoModel mojo = MojoModel.load(tmp.toString());

            EasyPredictModelWrapper wrapper = new EasyPredictModelWrapper(
                    new EasyPredictModelWrapper.Config()
                            .setModel(mojo)
                            .setConvertUnknownCategoricalLevelsToNa(true)
                            .setConvertInvalidNumbersToNa(true)
            );

            log.info("model_loaded: house={} area={} stream={} version={} threshold={}",
                    houseId, areaId, stream, version, threshold);

            return Optional.of(new ModelBundle(modelId, version, stream,
                    threshold, orderedFeatures, wrapper));

        } catch (Exception e) {
            log.error("model_load_error: house={} area={} stream={} err={}",
                    houseId, areaId, stream, e.getMessage());
            return Optional.empty();

        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); }
                catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void invalidateCache(String houseId, String areaId) {
        for (String stream : List.of("power", "water")) {
            cache.invalidate(cacheKey(houseId, areaId, stream));
        }
        log.info("cache_invalidated: house={} area={}", houseId, areaId);
    }

    private static String cacheKey(String houseId, String areaId, String stream) {
        return areaId == null || areaId.isBlank()
                ? "house:%s|stream:%s".formatted(houseId, stream)
                : "house:%s|area:%s|stream:%s".formatted(houseId, areaId, stream);
    }
}
