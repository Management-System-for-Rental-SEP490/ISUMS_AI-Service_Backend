package com.isums.aiservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TranslationCache {

    private final Cache<String, TranslationOutcome> cache;

    public TranslationCache(
            @Value("${ai.translation.cache.ttl-hours:24}") long ttlHours,
            @Value("${ai.translation.cache.max-size:10000}") long maxSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(ttlHours))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }

    public TranslationOutcome getIfPresent(String text, String source, String target, String intent, boolean customerFacing) {
        return cache.getIfPresent(key(text, source, target, intent, customerFacing));
    }

    public void put(String text, String source, String target, String intent, boolean customerFacing, TranslationOutcome outcome) {
        if (outcome == null || !outcome.isDone()) return;
        cache.put(key(text, source, target, intent, customerFacing), outcome);
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }

    private String key(String text, String source, String target, String intent, boolean customerFacing) {
        return Integer.toHexString((text == null ? "" : text).hashCode())
                + '|' + (source == null ? "auto" : source.toLowerCase())
                + '|' + (target == null ? "" : target.toLowerCase())
                + '|' + (intent == null ? "" : intent)
                + '|' + customerFacing;
    }
}
