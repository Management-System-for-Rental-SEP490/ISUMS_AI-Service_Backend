package com.isums.aiservice.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IssueTranslationPostProcessor")
class IssueTranslationPostProcessorTest {

    private final IssueTranslationPostProcessor processor = new IssueTranslationPostProcessor();

    @Test
    @DisplayName("rewrites repair confirmation sentence for English")
    void rewritesEnglish() {
        String result = processor.refine(
                "Tôi đã kiểm tra, vui lòng xác nhận để tiến hành sửa chữa",
                "I have checked, please confirm to proceed with repair",
                "vi",
                "en"
        );

        assertThat(result).isEqualTo("I have checked it. Please confirm so we can proceed with the repair.");
    }

    @Test
    @DisplayName("rewrites repair confirmation sentence for Japanese")
    void rewritesJapanese() {
        String result = processor.refine(
                "Tôi đã kiểm tra, vui lòng xác nhận để tiến hành sửa chữa",
                "確認しました。確認して修理を続行してください",
                "vi",
                "ja"
        );

        assertThat(result).isEqualTo("確認いたしました。修理を進めるため、ご確認をお願いいたします。");
    }

    @Test
    @DisplayName("leaves unrelated translation unchanged")
    void leavesUnrelatedText() {
        String result = processor.refine(
                "Đèn đã thay xong",
                "The light has been replaced",
                "vi",
                "en"
        );

        assertThat(result).isEqualTo("The light has been replaced");
    }
}
