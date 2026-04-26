package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.CustomerFacingPolishResult;
import com.isums.aiservice.domains.dtos.TextTranslationResult;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.infrastructures.abstracts.TextTranslationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranslationOrchestratorTest {

    private TextTranslationService translator;
    private TranslationPolicyResolver policyResolver;
    private CustomerFacingTranslationPolisher polisher;
    private IssueTranslationPostProcessor postProcessor;
    private TranslationCache cache;
    private TranslationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        translator = mock(TextTranslationService.class);
        policyResolver = mock(TranslationPolicyResolver.class);
        polisher = mock(CustomerFacingTranslationPolisher.class);
        postProcessor = mock(IssueTranslationPostProcessor.class);
        cache = mock(TranslationCache.class);
        TranslationLocaleSupport localeSupport = new TranslationLocaleSupport();

        when(policyResolver.resolve(any())).thenReturn(
                new TranslationPolicy(false, false, false, List.of()));
        when(polisher.polish(anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> new CustomerFacingPolishResult(
                        inv.getArgument(1), false));
        when(postProcessor.refine(anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        orchestrator = new TranslationOrchestrator(
                translator,
                policyResolver,
                polisher,
                postProcessor,
                localeSupport,
                cache,
                new SimpleMeterRegistry());
    }

    @Test
    void successfulTranslationProducesDoneOutcome() {
        when(translator.translate(anyString(), anyString(), anyString(), any()))
                .thenReturn(new TextTranslationResult("vi", "en", "Hello", "aws-translate", "DONE"));

        TranslationOutcome outcome = orchestrator.translateOne(
                "Xin chào", "vi", "en", "notification.title", "CUSTOMER_FACING_UI", true);

        assertThat(outcome.status()).isEqualTo("DONE");
        assertThat(outcome.translatedText()).isEqualTo("Hello");
        assertThat(outcome.targetLanguage()).isEqualTo("en");
        verify(cache, times(1)).put(anyString(), anyString(), anyString(), any(), anyBoolean(), any());
    }

    @Test
    void awsFailureBecomesFailedOutcomeNotCached() {
        when(translator.translate(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("AWS timeout"));

        TranslationOutcome outcome = orchestrator.translateOne(
                "Xin chào", "vi", "ja", "issue-ticket.note", "STAFF_INTERNAL", false);

        assertThat(outcome.status()).isEqualTo("FAILED");
        assertThat(outcome.translatedText()).isNull();
        assertThat(outcome.errorMessage()).contains("AWS timeout");
        verify(cache, never()).put(anyString(), anyString(), anyString(), any(), anyBoolean(), any());
    }

    @Test
    void cacheHitSkipsTranslator() {
        TranslationOutcome cached = new TranslationOutcome(
                "vi", "en", "Cached Hello", "aws-translate", "DONE", null);
        when(cache.getIfPresent(anyString(), anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(cached);

        TranslationOutcome outcome = orchestrator.translateOne(
                "Xin chào", "vi", "en", "house.name", null, false);

        assertThat(outcome).isSameAs(cached);
        verify(translator, never()).translate(anyString(), anyString(), anyString(), any());
    }

    @Test
    void translateAllDedupesTargetsAndNormalises() {
        when(translator.translate(anyString(), anyString(), anyString(), any()))
                .thenAnswer(inv -> new TextTranslationResult(
                        "vi", inv.getArgument(2), "T-" + inv.getArgument(2), "aws-translate", "DONE"));

        var result = orchestrator.translateAll(
                "Xin chào", "vi", List.of("EN", "en-US", "ja-JP", "ja"),
                "notification.title", "CUSTOMER_FACING_UI", true);

        assertThat(result).containsOnlyKeys("en", "ja");
        assertThat(result.get("en").translatedText()).isEqualTo("T-en");
        assertThat(result.get("ja").translatedText()).isEqualTo("T-ja");
    }

    @Test
    void polisherMarksProviderWhenBedrockUsed() {
        when(translator.translate(anyString(), anyString(), anyString(), any()))
                .thenReturn(new TextTranslationResult("vi", "ja", "こんにちは", "aws-translate", "DONE"));
        when(polisher.polish(anyString(), anyString(), any(), any()))
                .thenReturn(new CustomerFacingPolishResult("こんにちは(polished)", true));

        TranslationOutcome outcome = orchestrator.translateOne(
                "Xin chào", "vi", "ja", "notification.title", "CUSTOMER_FACING_UI", true);

        assertThat(outcome.provider()).isEqualTo("aws-translate+bedrock-polish");
        assertThat(outcome.translatedText()).isEqualTo("こんにちは(polished)");
    }

    private static boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }
}
