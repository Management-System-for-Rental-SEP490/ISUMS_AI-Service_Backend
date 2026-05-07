package com.isums.aiservice.infrastructures.kafka;

import com.isums.common.i18n.events.TextTranslationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TextTranslationResultProducer {

    private static final String DEFAULT_CALLBACK_PREFIX = "text.translation.result.";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String callbackTopic, TextTranslationResultEvent event) {
        String topic = resolveTopic(callbackTopic, event);
        String key = event.resourceId() + ":" + event.targetLanguage();
        kafkaTemplate.send(topic, key, event);
        log.debug("Published translation result requestId={} topic={} status={}",
                event.requestId(), topic, event.status());
    }

    private static String resolveTopic(String callbackTopic, TextTranslationResultEvent event) {
        if (callbackTopic != null && !callbackTopic.isBlank()) {
            return callbackTopic;
        }
        String resourceType = event.resourceType();
        if (resourceType == null || resourceType.isBlank()) {
            return DEFAULT_CALLBACK_PREFIX + "unknown";
        }
        String service = resourceType.split("\\.", 2)[0];
        return DEFAULT_CALLBACK_PREFIX + service;
    }
}
