package com.isums.aiservice.infrastructures.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.aiservice.domains.dtos.IssueTextTranslationRequestedEvent;
import com.isums.aiservice.domains.dtos.TextTranslationResult;
import com.isums.aiservice.infrastructures.abstracts.TextTranslationService;
import com.isums.aiservice.infrastructures.kafka.IssueTextTranslationResultProducer;
import com.isums.aiservice.services.IssueTranslationPostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueTextTranslationListener")
class IssueTextTranslationListenerTest {

    @Mock private TextTranslationService textTranslationService;
    @Mock private IssueTextTranslationResultProducer resultProducer;
    @Mock private Acknowledgment acknowledgment;

    @Test
    @DisplayName("translates request and publishes result event")
    void translatesAndPublishes() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        IssueTextTranslationListener listener = new IssueTextTranslationListener(
                objectMapper,
                textTranslationService,
                resultProducer,
                new IssueTranslationPostProcessor()
        );

        UUID resourceId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(textTranslationService.translate("Tôi đã kiểm tra, vui lòng xác nhận để tiến hành sửa chữa", null, "ja"))
                .thenReturn(new TextTranslationResult("vi", "ja", "確認しました。確認して修理を続行してください", "aws-translate", "DONE"));

        String payload = objectMapper.writeValueAsString(new IssueTextTranslationRequestedEvent(
                requestId,
                "EXECUTION",
                resourceId,
                "Tôi đã kiểm tra, vui lòng xác nhận để tiến hành sửa chữa",
                null,
                "ja",
                Instant.now()
        ));

        listener.onRequested(payload, acknowledgment);

        ArgumentCaptor<com.isums.aiservice.domains.dtos.IssueTextTranslationResultEvent> cap =
                ArgumentCaptor.forClass(com.isums.aiservice.domains.dtos.IssueTextTranslationResultEvent.class);
        verify(resultProducer).send(cap.capture());
        assertThat(cap.getValue().resourceId()).isEqualTo(resourceId);
        assertThat(cap.getValue().targetLanguage()).isEqualTo("ja");
        assertThat(cap.getValue().translatedText()).isEqualTo("確認いたしました。修理を進めるため、ご確認をお願いいたします。");
        assertThat(cap.getValue().status()).isEqualTo("DONE");
        verify(acknowledgment).acknowledge();
    }
}
