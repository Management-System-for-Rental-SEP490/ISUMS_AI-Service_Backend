package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.TextTranslationResult;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.infrastructures.abstracts.TextTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.Formality;
import software.amazon.awssdk.services.translate.model.TranslationSettings;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;

@Service
@RequiredArgsConstructor
public class AwsTextTranslationService implements TextTranslationService {

    private final TranslateClient translateClient;
    private final TranslationLocaleSupport localeSupport;

    @Override
    public TextTranslationResult translate(String text, String sourceLanguage, String targetLanguage, TranslationPolicy policy) {
        String normalizedTarget = localeSupport.normalize(targetLanguage);
        String normalizedSource = localeSupport.normalize(sourceLanguage);

        if (text == null || text.isBlank()) {
            return new TextTranslationResult(normalizedSource, normalizedTarget, text, "aws-translate", "SKIPPED");
        }

        if (normalizedSource != null && normalizedSource.equalsIgnoreCase(normalizedTarget)) {
            return new TextTranslationResult(normalizedSource, normalizedTarget, text, "aws-translate", "SKIPPED");
        }

        TranslateTextRequest.Builder requestBuilder = TranslateTextRequest.builder()
                .text(text)
                .sourceLanguageCode(normalizedSource == null || normalizedSource.isBlank() ? "auto" : normalizedSource)
                .targetLanguageCode(normalizedTarget);

        if (policy != null && policy.formalRequested()) {
            requestBuilder.settings(TranslationSettings.builder()
                    .formality(Formality.FORMAL)
                    .build());
        }

        if (policy != null && policy.terminologyNames() != null && !policy.terminologyNames().isEmpty()) {
            requestBuilder.terminologyNames(policy.terminologyNames());
        }

        var response = translateClient.translateText(requestBuilder.build());
        String detectedSource = localeSupport.normalize(response.sourceLanguageCode());
        String translatedText = response.translatedText();
        String provider = buildProviderTag(policy);

        if (detectedSource != null && detectedSource.equalsIgnoreCase(normalizedTarget)) {
            translatedText = text;
            return new TextTranslationResult(detectedSource, normalizedTarget, translatedText, provider, "SKIPPED");
        }

        return new TextTranslationResult(detectedSource, normalizedTarget, translatedText, provider, "DONE");
    }

    private String buildProviderTag(TranslationPolicy policy) {
        String provider = "aws-translate";
        if (policy == null) {
            return provider;
        }
        if (policy.formalRequested()) {
            provider += "+formal";
        }
        if (policy.terminologyNames() != null && !policy.terminologyNames().isEmpty()) {
            provider += "+terms";
        }
        return provider;
    }
}
