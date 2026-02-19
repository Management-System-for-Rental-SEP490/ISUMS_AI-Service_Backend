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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ModelStoreServiceImpl implements ModelStoreService {

    private final S3StoreClient s3StoreClient;
    private final ObjectMapper om;

    @Value("${ai.s3.bucket}")
    private String bucket;
    @Value("${ai.s3.prefix}")
    private String prefix;
    @Value("${ai.scoring.defaultThreshold}")
    private double defaultThreshold;
    @Value("${ai.cache.ttlSeconds}")
    private int ttlSeconds;

    private Cache<String, ModelBundle> cache;

    @PostConstruct
    void initCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(5000)
                .build();
    }

    @Override
    public ModelBundle getModelBundle(UUID houseId, UUID areaId) {
        String ck = cacheKey(houseId, areaId);
        return cache.get(ck, _k -> loadFromS3(houseId, areaId));
    }

    private static String cacheKey(UUID houseId, UUID areaId) {
        return areaId == null
                ? "house:" + houseId
                : "house:" + houseId + "|area:" + areaId;
    }

    private ModelBundle loadFromS3(UUID houseId, UUID areaId) {
        try {


            String lastestKey = ModelKeyResolver.lastestKey(prefix, houseId, areaId);
            JsonNode lastest = om.readTree(new String(s3StoreClient.getBytes(bucket, lastestKey), StandardCharsets.UTF_8));
            String version = lastest.get("version").asString(null);
            if (version == null || version.isBlank()) {
                throw new RuntimeException("lastest.json missing version");
            }

            String metaKey = ModelKeyResolver.metaKey(prefix, houseId, areaId, version);
            JsonNode meta = om.readTree(new String(s3StoreClient.getBytes(bucket, metaKey), StandardCharsets.UTF_8));

            String modelId = meta.path("modelId").asString("house_" + houseId + "_" + version);

            double threshold = meta.path("threshold").path("scoreThreshold").asDouble(defaultThreshold);

            List<String> orderedFeatures = new ArrayList<>();
            JsonNode arr = meta.path("features").path("ordered");

            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    orderedFeatures.add(node.asString());
                }
            }

            if (orderedFeatures.isEmpty()) {
                throw new RuntimeException("meta.json missing features.ordered");
            }

            String mojoKey = ModelKeyResolver.mojoKey(prefix, houseId, areaId, version);
            byte[] mojoBytes = s3StoreClient.getBytes(bucket, mojoKey);

            Path tmp = Files.createTempFile("eif_", ".mojo");
            Files.write(tmp, mojoBytes);
            tmp.toFile().deleteOnExit();
            MojoModel mojo = MojoModel.load(tmp.toString());

            EasyPredictModelWrapper wrapper = new EasyPredictModelWrapper(new EasyPredictModelWrapper.Config()
                    .setModel(mojo)
                    .setConvertUnknownCategoricalLevelsToNa(true)
                    .setConvertInvalidNumbersToNa(true)
            );

            return new ModelBundle(modelId, version, threshold, orderedFeatures, wrapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load model for house=%s area=%s: %s".formatted(houseId, areaId, e.getMessage()), e);
        }
    }

    @Override
    public void invalidateCache(UUID houseId, UUID areaId) {
        cache.invalidate(cacheKey(houseId, areaId));
    }

}
