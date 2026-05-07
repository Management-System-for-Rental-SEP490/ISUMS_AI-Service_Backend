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

    private static final Set<String> CUSTOMER_FACING_INTENTS = Set.of(
            "CUSTOMER_FACING_UI",
            "CUSTOMER_REPLY",
            "TENANT_REPLY",
            "QUESTION_RESPONSE",
            "APPROVAL_REQUEST"
    );

    private static final String INTENT_LEGAL = "LEGAL_REFERENCE";

    private final TranslationProperties translationProperties;
    private final TranslationLocaleSupport localeSupport;

    public TranslationPolicy resolve(TranslationRequestContext context) {
        String normalizedTarget = localeSupport.normalize(context.targetLanguage());
        String intent = normalizeIntent(context.translationIntent());
        boolean legalIntent = INTENT_LEGAL.equals(intent);

        boolean customerFacing = !legalIntent
                && (context.customerFacing() || CUSTOMER_FACING_INTENTS.contains(intent));

        boolean formalRequested = customerFacing
                && normalizedTarget != null
                && FORMALITY_SUPPORTED_TARGETS.contains(normalizedTarget.toLowerCase(Locale.ROOT));

        boolean bedrockPolish = customerFacing
                && !legalIntent
                && translationProperties.getCustomerFacing().isBedrockEnabled();

        List<String> terminologyNames = translationProperties.getTerminologyNames() == null
                ? List.of()
                : translationProperties.getTerminologyNames().stream()
                    .filter(name -> name != null && !name.isBlank())
                    .toList();

        return new TranslationPolicy(
                customerFacing,
                formalRequested,
                bedrockPolish,
                terminologyNames
        );
    }

    private static String normalizeIntent(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
