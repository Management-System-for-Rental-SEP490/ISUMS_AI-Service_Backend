package com.isums.aiservice.infrastructures.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import com.isums.aiservice.infrastructures.kafka.TextTranslationResultProducer;
import com.isums.aiservice.services.TranslationOrchestrator;
import com.isums.common.i18n.events.TextTranslationRequestedEvent;
import com.isums.common.i18n.events.TextTranslationResultEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextTranslationListenerTest {

    private ObjectMapper mapper;
    private TranslationOrchestrator orchestrator;
    private TextTranslationResultProducer producer;
    private Acknowledgment ack;
    private TextTranslationListener listener;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        orchestrator = mock(TranslationOrchestrator.class);
        producer = mock(TextTranslationResultProducer.class);
        ack = mock(Acknowledgment.class);
        listener = new TextTranslationListener(mapper, orchestrator, producer);
    }

    @Test
    void publishesOneResultPerTargetAndAcks() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        TextTranslationRequestedEvent event = new TextTranslationRequestedEvent(
                requestId, "notification.title", resourceId, "title",
                "Xin chào", "vi", List.of("en", "ja"),
                "CUSTOMER_FACING_UI", true, Instant.now(),
                "text.translation.result.notification");

        when(orchestrator.translateOne(anyString(), anyString(), eq("en"), anyString(), anyString(), any()))
                .thenReturn(new TranslationOutcome("vi", "en", "Hello", "aws-translate", "DONE", null));
        when(orchestrator.translateOne(anyString(), anyString(), eq("ja"), anyString(), anyString(), any()))
                .thenReturn(new TranslationOutcome("vi", "ja", "こんにちは", "aws-translate+formal", "DONE", null));

        listener.onRequested(mapper.writeValueAsString(event), ack);

        ArgumentCaptor<TextTranslationResultEvent> resultCaptor = ArgumentCaptor.forClass(TextTranslationResultEvent.class);
        verify(producer, times(2)).send(eq("text.translation.result.notification"), resultCaptor.capture());
        assertThat(resultCaptor.getAllValues())
                .extracting(TextTranslationResultEvent::targetLanguage, TextTranslationResultEvent::translatedText)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("en", "Hello"),
                        org.assertj.core.groups.Tuple.tuple("ja", "こんにちは"));
        verify(ack).acknowledge();
    }

    @Test
    void orchestratorFailurePerTargetPublishesFailedResult() throws Exception {
        TextTranslationRequestedEvent event = new TextTranslationRequestedEvent(
                UUID.randomUUID(), "notification.title", UUID.randomUUID(), "title",
                "Xin chào", "vi", List.of("en"),
                "CUSTOMER_FACING_UI", true, Instant.now(),
                "text.translation.result.notification");

        when(orchestrator.translateOne(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("boom"));

        listener.onRequested(mapper.writeValueAsString(event), ack);

        ArgumentCaptor<TextTranslationResultEvent> resultCaptor = ArgumentCaptor.forClass(TextTranslationResultEvent.class);
        verify(producer).send(anyString(), resultCaptor.capture());
        TextTranslationResultEvent result = resultCaptor.getValue();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).isEqualTo("boom");
        assertThat(result.translatedText()).isNull();
        verify(ack).acknowledge();
    }

    @Test
    void malformedPayloadIsAckedWithoutPublishing() {
        listener.onRequested("not-json", ack);

        verify(producer, never()).send(anyString(), any());
        verify(ack).acknowledge();
    }

    @Test
    void emptyTargetsAckedWithoutPublishing() throws Exception {
        TextTranslationRequestedEvent event = new TextTranslationRequestedEvent(
                UUID.randomUUID(), "notification.title", UUID.randomUUID(), "title",
                "Xin chào", "vi", List.of(),
                null, false, Instant.now(), "x");

        listener.onRequested(mapper.writeValueAsString(event), ack);

        verify(producer, never()).send(anyString(), any());
        verify(ack).acknowledge();
    }
}
