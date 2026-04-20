package com.isums.aiservice.infrastructures.kafka;

import com.isums.aiservice.domains.dtos.IssueTextTranslationResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTextTranslationResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(IssueTextTranslationResultEvent event) {
        kafkaTemplate.send("issue.text.translation.result", event.resourceId() + ":" + event.targetLanguage(), event);
    }
}
