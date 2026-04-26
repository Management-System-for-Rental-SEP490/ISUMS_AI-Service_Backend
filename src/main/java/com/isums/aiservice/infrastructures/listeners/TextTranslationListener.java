package com.isums.aiservice.infrastructures.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import com.isums.aiservice.infrastructures.kafka.TextTranslationResultProducer;
import com.isums.aiservice.services.TranslationOrchestrator;
import com.isums.common.i18n.events.TextTranslationRequestedEvent;
import com.isums.common.i18n.events.TextTranslationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TextTranslationListener {

    private final ObjectMapper objectMapper;
    private final TranslationOrchestrator orchestrator;
    private final TextTranslationResultProducer resultProducer;

    @KafkaListener(topics = "text.translation.requested", groupId = "ai-text-translation-group")
    public void onRequested(String payload, Acknowledgment acknowledgment) {
        TextTranslationRequestedEvent event = null;
        try {
            event = objectMapper.readValue(payload, TextTranslationRequestedEvent.class);
        } catch (Exception parseEx) {
            log.error("Malformed TextTranslationRequestedEvent payload dropped: {}", payload, parseEx);
            acknowledgment.acknowledge();
            return;
        }

        List<String> targets = event.targetLanguages();
        if (targets == null || targets.isEmpty()) {
            log.warn("Rejecting translation request with empty targetLanguages requestId={}", event.requestId());
            acknowledgment.acknowledge();
            return;
        }

        for (String target : targets) {
            publishOutcome(event, target);
        }
        acknowledgment.acknowledge();
    }

    private void publishOutcome(TextTranslationRequestedEvent event, String target) {
        TranslationOutcome outcome;
        try {
            outcome = orchestrator.translateOne(
                    event.text(),
                    event.sourceLanguage(),
                    target,
                    event.resourceType(),
                    event.translationIntent(),
                    event.customerFacing());
        } catch (Exception ex) {
            log.error("Orchestrator threw for requestId={} target={}", event.requestId(), target, ex);
            outcome = new TranslationOutcome(
                    event.sourceLanguage(),
                    target,
                    null,
                    "aws-translate",
                    TextTranslationResultEvent.STATUS_FAILED,
                    ex.getMessage());
        }
        TextTranslationResultEvent result = new TextTranslationResultEvent(
                event.requestId(),
                event.resourceType(),
                event.resourceId(),
                event.fieldName(),
                outcome.sourceLanguage(),
                outcome.targetLanguage(),
                outcome.translatedText(),
                outcome.provider(),
                outcome.status(),
                outcome.errorMessage(),
                Instant.now());
        try {
            resultProducer.send(event.callbackTopic(), result);
        } catch (Exception publishEx) {
            log.error("Failed to publish translation result requestId={} target={}",
                    event.requestId(), target, publishEx);
        }
    }
}
