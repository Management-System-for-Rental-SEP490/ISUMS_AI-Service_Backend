package com.isums.aiservice.services;

import com.isums.aiservice.configs.TranslationProperties;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationPolicyResolverTest {

    private TranslationProperties properties;
    private TranslationPolicyResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new TranslationProperties();
        properties.setTerminologyNames(List.of("isums-glossary"));
        properties.getCustomerFacing().setBedrockEnabled(true);
        resolver = new TranslationPolicyResolver(properties, new TranslationLocaleSupport());
    }

    @Test
    void customerFacingUiIntentRequestsFormalToneAndBedrockPolish() {
        TranslationPolicy policy = resolver.resolve(new TranslationRequestContext(
                "notification.title", "CUSTOMER_FACING_UI", false, "vi", "ja"));

        assertThat(policy.customerFacing()).isTrue();
        assertThat(policy.formalRequested()).isTrue();
        assertThat(policy.bedrockPolishEnabled()).isTrue();
        assertThat(policy.terminologyNames()).containsExactly("isums-glossary");
    }

    @Test
    void legalIntentDisablesPolishEvenWhenCustomerFacingFlagIsTrue() {
        TranslationPolicy policy = resolver.resolve(new TranslationRequestContext(
                "econtract.name", "LEGAL_REFERENCE", true, "vi", "ja"));

        assertThat(policy.customerFacing()).isFalse();
        assertThat(policy.formalRequested()).isFalse();
        assertThat(policy.bedrockPolishEnabled()).isFalse();
    }

    @Test
    void staffInternalIntentIsCasualAndUnpolished() {
        TranslationPolicy policy = resolver.resolve(new TranslationRequestContext(
                "issue-ticket.note", "STAFF_INTERNAL", false, "vi", "en"));

        assertThat(policy.customerFacing()).isFalse();
        assertThat(policy.formalRequested()).isFalse();
        assertThat(policy.bedrockPolishEnabled()).isFalse();
    }

    @Test
    void nullIntentFallsBackToCustomerFacingFlag() {
        TranslationPolicy policy = resolver.resolve(new TranslationRequestContext(
                "anything", null, true, "vi", "ja"));

        assertThat(policy.customerFacing()).isTrue();
        assertThat(policy.formalRequested()).isTrue();
    }

    @Test
    void formalToneOnlyRequestedForSupportedTargets() {
        TranslationPolicy jaPolicy = resolver.resolve(new TranslationRequestContext(
                "notification.body", "CUSTOMER_FACING_UI", false, "vi", "ja"));
        TranslationPolicy enPolicy = resolver.resolve(new TranslationRequestContext(
                "notification.body", "CUSTOMER_FACING_UI", false, "vi", "en"));

        assertThat(jaPolicy.formalRequested()).isTrue();
        assertThat(enPolicy.formalRequested())
                .as("English is not in AWS Translate's formality-supported list")
                .isFalse();
    }

    @Test
    void bedrockDisabledGloballyOverridesIntent() {
        properties.getCustomerFacing().setBedrockEnabled(false);
        TranslationPolicy policy = resolver.resolve(new TranslationRequestContext(
                "notification.title", "CUSTOMER_FACING_UI", false, "vi", "ja"));

        assertThat(policy.customerFacing()).isTrue();
        assertThat(policy.bedrockPolishEnabled()).isFalse();
    }
}
