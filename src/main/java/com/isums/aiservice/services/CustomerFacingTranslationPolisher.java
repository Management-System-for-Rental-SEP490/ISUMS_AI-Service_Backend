package com.isums.aiservice.services;

import com.isums.aiservice.configs.TranslationProperties;
import com.isums.aiservice.domains.dtos.CustomerFacingPolishResult;
import com.isums.aiservice.domains.dtos.TranslationPolicy;
import com.isums.aiservice.domains.dtos.TranslationRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerFacingTranslationPolisher {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final TranslationProperties translationProperties;

    public CustomerFacingPolishResult polish(
            String originalText,
            String candidateTranslation,
            TranslationRequestContext context,
            TranslationPolicy policy
    ) {
        if (candidateTranslation == null || candidateTranslation.isBlank()) {
            return new CustomerFacingPolishResult(candidateTranslation, false);
        }
        if (policy == null || !policy.customerFacing() || !policy.bedrockPolishEnabled()) {
            return new CustomerFacingPolishResult(candidateTranslation, false);
        }

        try {
            Message message = Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromText(buildUserPrompt(originalText, candidateTranslation, context)))
                    .build();

            ConverseRequest request = ConverseRequest.builder()
                    .modelId(translationProperties.getCustomerFacing().getBedrockModelId())
                    .system(SystemContentBlock.builder().text(buildSystemPrompt(context)).build())
                    .messages(message)
                    .inferenceConfig(config -> config
                            .maxTokens(translationProperties.getCustomerFacing().getBedrockMaxTokens())
                            .temperature(translationProperties.getCustomerFacing().getBedrockTemperature()))
                    .build();

            String polished = bedrockRuntimeClient.converse(request)
                    .output()
                    .message()
                    .content()
                    .stream()
                    .map(ContentBlock::text)
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .orElse(candidateTranslation)
                    .trim();

            return new CustomerFacingPolishResult(polished.isBlank() ? candidateTranslation : polished, true);
        } catch (Exception ex) {
            log.warn("Bedrock polish fallback resourceType={} intent={} target={}: {}",
                    context.resourceType(), context.translationIntent(), context.targetLanguage(), ex.getMessage());
            return new CustomerFacingPolishResult(candidateTranslation, false);
        }
    }

    private String buildSystemPrompt(TranslationRequestContext context) {
        StringBuilder prompt = new StringBuilder("""
You are a production translation quality editor for ISUMS customer support.
Rewrite the candidate translation into natural, polite, customer-facing language.
Rules:
1. The candidate translation is the semantic baseline. Edit it for tone and fluency, not for new meaning.
2. Preserve exact business meaning, requested action, nouns, verbs, and factual scope.
3. Do not add promises, deadlines, apologies, blame, workflow terms, or new facts.
4. Do not replace nouns or actions with different ones unless the source text explicitly contains them.
5. Keep domain terms stable: tenant, landlord, manager, issue, inspection, repair, contract.
6. Make the smallest possible edits needed for a polite customer-facing result.
7. Return only the final translated text. No notes, no JSON, no quotes.
""");

        String targetLanguage = context.targetLanguage() == null ? "" : context.targetLanguage().trim().toLowerCase(Locale.ROOT);
        if ("ja".equals(targetLanguage)) {
            prompt.append("""
Japanese style guide:
- Use polite customer-service register.
- Prefer natural respectful requests over blunt imperatives.
- Keep the sentence concise and professional.

Example:
Candidate: 確認しました。確認して修理を続行してください
Final: 確認いたしました。修理を進めるため、ご確認をお願いいたします。
""");
        } else if ("en".equals(targetLanguage)) {
            prompt.append("""
English style guide:
- Use concise courteous customer-support tone.
- Keep the response natural, calm, and direct.

Example:
Candidate: I have checked, please confirm to proceed with repair.
Final: I have checked it. Please confirm so we can proceed with the repair.
""");
        }

        return prompt.toString();
    }

    private String buildUserPrompt(String originalText, String candidateTranslation, TranslationRequestContext context) {
        return """
Candidate translation:
%s

Source text:
%s

Target language: %s
Intent: %s
Return the final translation only.
""".formatted(
                candidateTranslation.trim(),
                originalText == null ? "" : originalText.trim(),
                context.targetLanguage() == null ? "" : context.targetLanguage().trim(),
                context.translationIntent() == null ? "" : context.translationIntent().trim()
        );
    }
}
