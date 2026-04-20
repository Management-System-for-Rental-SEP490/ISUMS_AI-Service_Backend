package com.isums.aiservice.infrastructures.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.aiservice.domains.dtos.IssueTextTranslationRequestedEvent;
import com.isums.aiservice.domains.dtos.IssueTextTranslationResultEvent;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import com.isums.aiservice.infrastructures.abstracts.TextTranslationService;
import com.isums.aiservice.infrastructures.kafka.IssueTextTranslationResultProducer;
import com.isums.aiservice.services.CustomerFacingTranslationPolisher;
import com.isums.aiservice.services.IssueTranslationPostProcessor;
import com.isums.aiservice.services.TranslationPolicyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class IssueTextTranslationListener {

    private final ObjectMapper objectMapper;
    private final TextTranslationService textTranslationService;
    private final IssueTextTranslationResultProducer resultProducer;
    private final TranslationPolicyResolver translationPolicyResolver;
    private final CustomerFacingTranslationPolisher customerFacingTranslationPolisher;
    private final IssueTranslationPostProcessor postProcessor;

    @KafkaListener(topics = "issue.text.translation.requested", groupId = "ai-translation-group")
    public void onRequested(String payload, Acknowledgment acknowledgment) {
        try {
            IssueTextTranslationRequestedEvent event = objectMapper.readValue(payload, IssueTextTranslationRequestedEvent.class);
            TranslationRequestContext context = new TranslationRequestContext(
                    event.resourceType(),
                    event.translationIntent(),
                    Boolean.TRUE.equals(event.customerFacing()),
                    event.sourceLanguage(),
                    event.targetLanguage()
            );
            var policy = translationPolicyResolver.resolve(context);
            var translated = textTranslationService.translate(event.text(), event.sourceLanguage(), event.targetLanguage(), policy);
            var polishResult = customerFacingTranslationPolisher.polish(
                    event.text(),
                    translated.translatedText(),
                    context,
                    policy
            );
            String refinedText = postProcessor.refine(polishResult.text(), context, policy);
            String provider = translated.provider();
            if (polishResult.usedBedrock()) {
                provider = provider + "+bedrock-polish";
            }

            resultProducer.send(IssueTextTranslationResultEvent.builder()
                    .requestId(event.requestId())
                    .resourceType(event.resourceType())
                    .resourceId(event.resourceId())
                    .sourceLanguage(translated.sourceLanguage())
                    .targetLanguage(translated.targetLanguage())
                    .translatedText(refinedText)
                    .provider(provider)
                    .status(translated.status())
                    .translatedAt(Instant.now())
                    .build());

            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to translate payload={}", payload, ex);
            try {
                IssueTextTranslationRequestedEvent event = objectMapper.readValue(payload, IssueTextTranslationRequestedEvent.class);
                resultProducer.send(IssueTextTranslationResultEvent.builder()
                        .requestId(event.requestId())
                        .resourceType(event.resourceType())
                        .resourceId(event.resourceId())
                        .sourceLanguage(event.sourceLanguage())
                        .targetLanguage(event.targetLanguage())
                        .provider("aws-translate")
                        .status("FAILED")
                        .errorMessage(ex.getMessage())
                        .translatedAt(Instant.now())
                        .build());
            } catch (Exception parseEx) {
                log.error("Failed to parse payload for failure handling payload={}", payload, parseEx);
            }
            acknowledgment.acknowledge();
        }
    }
}
