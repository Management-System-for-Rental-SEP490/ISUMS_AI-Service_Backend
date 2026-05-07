package com.isums.aiservice.services;

import com.isums.aiservice.domains.dtos.TextTranslationResult;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.Formality;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AwsTextTranslationService")
class AwsTextTranslationServiceTest {

    @Test
    @DisplayName("applies formal style and terminology for customer-facing japanese translation")
    void translates() {
        TranslateClient client = mock(TranslateClient.class);
        when(client.translateText(any(TranslateTextRequest.class))).thenReturn(
                TranslateTextResponse.builder()
                        .sourceLanguageCode("vi")
                        .targetLanguageCode("ja")
                        .translatedText("修理を承認してください")
                        .build()
        );

        AwsTextTranslationService service = new AwsTextTranslationService(client, new TranslationLocaleSupport());

        TextTranslationResult result = service.translate(
                "Vui long chap nhan sua chua",
                null,
                "ja-JP",
                new TranslationPolicy(true, true, true, java.util.List.of("isums_domain"))
        );

        ArgumentCaptor<TranslateTextRequest> requestCaptor = ArgumentCaptor.forClass(TranslateTextRequest.class);
        verify(client).translateText(requestCaptor.capture());

        assertThat(result.sourceLanguage()).isEqualTo("vi");
        assertThat(result.targetLanguage()).isEqualTo("ja");
        assertThat(result.translatedText()).isEqualTo("修理を承認してください");
        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.provider()).contains("aws-translate").contains("formal").contains("terms");
        assertThat(requestCaptor.getValue().settings().formality()).isEqualTo(Formality.FORMAL);
        assertThat(requestCaptor.getValue().terminologyNames()).containsExactly("isums_domain");
    }

    @Test
    @DisplayName("skips when source and target are the same")
    void skipsSameLanguage() {
        AwsTextTranslationService service = new AwsTextTranslationService(mock(TranslateClient.class), new TranslationLocaleSupport());

        TextTranslationResult result = service.translate(
                "Done",
                "en",
                "en-US",
                new TranslationPolicy(false, false, false, java.util.List.of())
        );

        assertThat(result.translatedText()).isEqualTo("Done");
        assertThat(result.status()).isEqualTo("SKIPPED");
    }
}
