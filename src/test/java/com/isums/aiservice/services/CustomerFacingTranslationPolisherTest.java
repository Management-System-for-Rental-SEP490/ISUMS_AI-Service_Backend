package com.isums.aiservice.services;

import com.isums.aiservice.configs.TranslationProperties;
import com.isums.aiservice.domains.dtos.CustomerFacingPolishResult;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomerFacingTranslationPolisher")
class CustomerFacingTranslationPolisherTest {

    @Test
    @DisplayName("uses Bedrock to polish customer-facing output")
    void polishesWithBedrock() {
        BedrockRuntimeClient client = mock(BedrockRuntimeClient.class);
        when(client.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .role(ConversationRole.ASSISTANT)
                                .content(ContentBlock.fromText("確認いたしました。修理を進めるため、ご確認をお願いいたします。"))
                                .build())
                        .build())
                .build());

        TranslationProperties properties = new TranslationProperties();
        CustomerFacingTranslationPolisher polisher = new CustomerFacingTranslationPolisher(client, properties);

        CustomerFacingPolishResult polished = polisher.polish(
                "Tôi đã kiểm tra, vui lòng xác nhận để tiến hành sửa chữa",
                "確認しました。確認して修理を続行してください",
                new TranslationRequestContext("RESPONSE", "QUESTION_RESPONSE", true, "vi", "ja"),
                new TranslationPolicy(true, true, true, java.util.List.of())
        );

        assertThat(polished.text()).isEqualTo("確認いたしました。修理を進めるため、ご確認をお願いいたします。");
        assertThat(polished.usedBedrock()).isTrue();
        verify(client).converse(any(ConverseRequest.class));
    }
}
