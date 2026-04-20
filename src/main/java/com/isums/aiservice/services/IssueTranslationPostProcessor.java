package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import org.springframework.stereotype.Component;

@Component
public class IssueTranslationPostProcessor {

    public String refine(String translatedText, TranslationRequestContext context, TranslationPolicy policy) {
        if (translatedText == null || translatedText.isBlank()) {
            return translatedText;
        }

        String refined = translatedText.trim().replaceAll("\\s+", " ");
        if (policy != null && policy.customerFacing() && context != null && "en".equalsIgnoreCase(context.targetLanguage())) {
            refined = refined
                    .replace("I have checked, ", "I have checked it. ")
                    .replace("please confirm to proceed with", "Please confirm so we can proceed with")
                    .replace("proceed with repair", "proceed with the repair");
        }
        if (policy != null && policy.customerFacing() && context != null && "ja".equalsIgnoreCase(context.targetLanguage())) {
            refined = refined
                    .replace("お願い致します", "お願いいたします")
                    .replace("下さい", "ください");
        }
        return refined;
    }
}
