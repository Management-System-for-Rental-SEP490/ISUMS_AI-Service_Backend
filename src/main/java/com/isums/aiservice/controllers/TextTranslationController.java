package com.isums.aiservice.controllers;

import com.isums.aiservice.domains.dtos.TranslateTextRequest;
import com.isums.aiservice.domains.dtos.TranslateTextResponse;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import com.isums.aiservice.services.TranslationOrchestrator;
import com.isums.common.i18n.events.TextTranslationResultEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/translate")
@RequiredArgsConstructor
public class TextTranslationController {

    private final TranslationOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<TranslateTextResponse> translate(@Valid @RequestBody TranslateTextRequest request) {
        Map<String, TranslationOutcome> outcomes = orchestrator.translateAll(
                request.text(),
                request.sourceLanguage(),
                request.targetLanguages(),
                request.resourceType(),
                request.intent(),
                request.customerFacing());

        Map<String, String> translations = new LinkedHashMap<>();
        Map<String, String> statuses = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        String provider = "aws-translate";
        for (Map.Entry<String, TranslationOutcome> entry : outcomes.entrySet()) {
            TranslationOutcome outcome = entry.getValue();
            translations.put(entry.getKey(), outcome.translatedText());
            statuses.put(entry.getKey(), outcome.status());
            if (TextTranslationResultEvent.STATUS_FAILED.equals(outcome.status()) && outcome.errorMessage() != null) {
                errors.put(entry.getKey(), outcome.errorMessage());
            }
            if (outcome.provider() != null) {
                provider = outcome.provider();
            }
        }
        return ResponseEntity.ok(new TranslateTextResponse(translations, statuses, provider, errors));
    }
}
