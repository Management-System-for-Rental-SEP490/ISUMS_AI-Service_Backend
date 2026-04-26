package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.TextTranslationResult;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import com.isums.aiservice.infrastructures.abstracts.TextTranslationService;
import com.isums.common.i18n.events.TextTranslationResultEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationOrchestrator {

    private static final String METRIC_REQUESTS = "ai.translation.requests";
    private static final String METRIC_DURATION = "ai.translation.duration";

    private final TextTranslationService translator;
    private final TranslationPolicyResolver policyResolver;
    private final CustomerFacingTranslationPolisher polisher;
    private final IssueTranslationPostProcessor postProcessor;
    private final TranslationLocaleSupport localeSupport;
    private final TranslationCache cache;
    private final MeterRegistry meterRegistry;

    /**
     * Translate {@code text} into each of {@code targetLanguages}. Returns a
     * map keyed by normalised locale; guaranteed to contain every requested
     * target, including {@code FAILED} entries for unrecoverable errors.
     */
    public Map<String, TranslationOutcome> translateAll(
            String text,
            String sourceLanguage,
            List<String> targetLanguages,
            String resourceType,
            String intent,
            Boolean customerFacing) {
        Map<String, TranslationOutcome> results = new LinkedHashMap<>();
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            return results;
        }
        for (String raw : targetLanguages) {
            String target = localeSupport.normalize(raw);
            if (target == null || target.isBlank()) {
                continue;
            }
            if (results.containsKey(target)) {
                continue;
            }
            results.put(target, translateOne(text, sourceLanguage, target, resourceType, intent, customerFacing));
        }
        return results;
    }

    public TranslationOutcome translateOne(
            String text,
            String sourceLanguage,
            String targetLanguage,
            String resourceType,
            String intent,
            Boolean customerFacing) {
        String normalizedSource = localeSupport.normalize(sourceLanguage);
        String normalizedTarget = localeSupport.normalize(targetLanguage);
        boolean customerFacingFlag = Boolean.TRUE.equals(customerFacing);

        TranslationOutcome cached = cache.getIfPresent(
                text, normalizedSource, normalizedTarget, intent, customerFacingFlag);
        if (cached != null) {
            incrementRequest(resourceType, normalizedTarget, cached.status(), "hit");
            return cached;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        TranslationOutcome outcome;
        try {
            outcome = doTranslate(text, normalizedSource, normalizedTarget, resourceType, intent, customerFacingFlag);
            cache.put(text, normalizedSource, normalizedTarget, intent, customerFacingFlag, outcome);
        } catch (Exception ex) {
            log.warn("Translation failed resourceType={} target={} err={}", resourceType, normalizedTarget, ex.toString());
            outcome = new TranslationOutcome(
                    normalizedSource,
                    normalizedTarget,
                    null,
                    "aws-translate",
                    TextTranslationResultEvent.STATUS_FAILED,
                    ex.getMessage());
        }
        sample.stop(meterRegistry.timer(METRIC_DURATION,
                "resource_type", safeTag(resourceType),
                "target", safeTag(normalizedTarget),
                "status", safeTag(outcome.status())));
        incrementRequest(resourceType, normalizedTarget, outcome.status(), "miss");
        return outcome;
    }

    private TranslationOutcome doTranslate(
            String text,
            String normalizedSource,
            String normalizedTarget,
            String resourceType,
            String intent,
            boolean customerFacing) {
        TranslationRequestContext context = new TranslationRequestContext(
                resourceType, intent, customerFacing, normalizedSource, normalizedTarget);
        TranslationPolicy policy = policyResolver.resolve(context);

        TextTranslationResult translated = translator.translate(text, normalizedSource, normalizedTarget, policy);

        String refinedText = translated.translatedText();
        String provider = translated.provider();
        String status = mapStatus(translated.status());

        if (TextTranslationResultEvent.STATUS_DONE.equals(status) && refinedText != null) {
            var polish = polisher.polish(text, refinedText, context, policy);
            refinedText = polish.text();
            if (polish.usedBedrock()) {
                provider = provider + "+bedrock-polish";
            }
            refinedText = postProcessor.refine(refinedText, context, policy);
        }

        return new TranslationOutcome(
                translated.sourceLanguage(),
                translated.targetLanguage(),
                refinedText,
                provider,
                status,
                null);
    }

    private void incrementRequest(String resourceType, String target, String status, String cacheOutcome) {
        meterRegistry.counter(METRIC_REQUESTS,
                "resource_type", safeTag(resourceType),
                "target", safeTag(target),
                "status", safeTag(status),
                "cache", cacheOutcome
        ).increment();
    }

    private static String safeTag(String raw) {
        return (raw == null || raw.isBlank()) ? "unknown" : raw;
    }

    private static String mapStatus(String provided) {
        if (provided == null) return TextTranslationResultEvent.STATUS_DONE;
        String upper = provided.toUpperCase();
        return switch (upper) {
            case "DONE", "SUCCESS" -> TextTranslationResultEvent.STATUS_DONE;
            case "SKIPPED" -> TextTranslationResultEvent.STATUS_SKIPPED;
            case "FAILED", "FAILURE" -> TextTranslationResultEvent.STATUS_FAILED;
            default -> upper;
        };
    }
}
