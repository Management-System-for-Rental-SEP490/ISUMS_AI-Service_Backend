package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IssueTranslationPostProcessor")
class IssueTranslationPostProcessorTest {

    private final IssueTranslationPostProcessor processor = new IssueTranslationPostProcessor();

    @Test
    @DisplayName("normalizes Japanese customer-facing wording variants")
    void normalizesJapaneseCustomerFacingWording() {
        String result = processor.refine(
                "ご確認をお願い致します。資料を送って下さい。",
                new TranslationRequestContext("RESPONSE", "QUESTION_RESPONSE", true, "vi", "ja"),
                new TranslationPolicy(true, true, true, java.util.List.of())
        );

        assertThat(result).isEqualTo("ご確認をお願いいたします。資料を送ってください。");
    }

    @Test
    @DisplayName("smooths awkward English confirmation phrasing")
    void smoothsAwkwardEnglishConfirmation() {
        String result = processor.refine(
                "I have checked, please confirm to proceed with repair",
                new TranslationRequestContext("RESPONSE", "QUESTION_RESPONSE", true, "vi", "en"),
                new TranslationPolicy(true, false, false, java.util.List.of())
        );

        assertThat(result).isEqualTo("I have checked it. Please confirm so we can proceed with the repair");
    }

    @Test
    @DisplayName("leaves unrelated translation unchanged")
    void leavesUnrelatedText() {
        String result = processor.refine(
                "The light has been replaced",
                new TranslationRequestContext("EXECUTION", "WORK_EXECUTION_NOTE", false, "vi", "en"),
                new TranslationPolicy(false, false, false, java.util.List.of())
        );

        assertThat(result).isEqualTo("The light has been replaced");
    }
}
