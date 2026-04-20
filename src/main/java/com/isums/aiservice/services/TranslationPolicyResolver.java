package com.isums.aiservice.services;

import com.isums.aiservice.configs.TranslationProperties;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TranslationPolicyResolver {

    private static final Set<String> FORMALITY_SUPPORTED_TARGETS = Set.of(
            "nl", "fr", "fr-ca", "de", "hi", "it", "ja", "ko", "pt-pt", "es", "es-mx"
    );

    private final TranslationProperties translationProperties;
    private final TranslationLocaleSupport localeSupport;

    public TranslationPolicy resolve(TranslationRequestContext context) {
        String normalizedTarget = localeSupport.normalize(context.targetLanguage());
        boolean customerFacing = context.customerFacing() || isCustomerFacingIntent(context.translationIntent());
        boolean formalRequested = customerFacing
                && normalizedTarget != null
                && FORMALITY_SUPPORTED_TARGETS.contains(normalizedTarget.toLowerCase(Locale.ROOT));

        List<String> terminologyNames = translationProperties.getTerminologyNames() == null
                ? List.of()
                : translationProperties.getTerminologyNames().stream()
                .filter(name -> name != null && !name.isBlank())
                .toList();

        return new TranslationPolicy(
                customerFacing,
                formalRequested,
                customerFacing && translationProperties.getCustomerFacing().isBedrockEnabled(),
                terminologyNames
        );
    }

    private boolean isCustomerFacingIntent(String translationIntent) {
        if (translationIntent == null || translationIntent.isBlank()) {
            return false;
        }
        return switch (translationIntent.trim().toUpperCase(Locale.ROOT)) {
            case "QUESTION_RESPONSE", "CUSTOMER_REPLY", "TENANT_REPLY", "APPROVAL_REQUEST" -> true;
            default -> false;
        };
    }
}
