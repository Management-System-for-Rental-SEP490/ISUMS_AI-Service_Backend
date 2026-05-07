package com.isums.aiservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.aiservice.domains.dtos.TranslationOutcome;
import com.isums.aiservice.services.TranslationOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TextTranslationController.class)
@AutoConfigureMockMvc(addFilters = false)
class TextTranslationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private TranslationOrchestrator orchestrator;

    @Test
    void returnsTranslationsForValidRequest() throws Exception {
        Map<String, TranslationOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put("en", new TranslationOutcome("vi", "en", "Hello", "aws-translate", "DONE", null));
        outcomes.put("ja", new TranslationOutcome("vi", "ja", "こんにちは", "aws-translate+formal", "DONE", null));
        when(orchestrator.translateAll(anyString(), eq("vi"), any(), anyString(), anyString(), any()))
                .thenReturn(outcomes);

        Map<String, Object> body = Map.of(
                "text", "Xin chào",
                "sourceLanguage", "vi",
                "targetLanguages", List.of("en", "ja"),
                "intent", "CUSTOMER_FACING_UI",
                "resourceType", "notification.title"
        );

        mvc.perform(post("/api/ai/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translations.en").value("Hello"))
                .andExpect(jsonPath("$.translations.ja").value("こんにちは"))
                .andExpect(jsonPath("$.statuses.en").value("DONE"))
                .andExpect(jsonPath("$.statuses.ja").value("DONE"));
    }

    @Test
    void rejectsBlankText() throws Exception {
        Map<String, Object> body = Map.of(
                "text", "   ",
                "targetLanguages", List.of("en")
        );

        mvc.perform(post("/api/ai/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyTargetList() throws Exception {
        Map<String, Object> body = Map.of(
                "text", "Xin chào",
                "targetLanguages", List.of()
        );

        mvc.perform(post("/api/ai/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void surfacesFailedErrorsInResponseBody() throws Exception {
        Map<String, TranslationOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put("en", new TranslationOutcome("vi", "en", "Hello", "aws-translate", "DONE", null));
        outcomes.put("ja", new TranslationOutcome("vi", "ja", null, "aws-translate", "FAILED", "AWS timed out"));
        when(orchestrator.translateAll(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(outcomes);

        Map<String, Object> body = Map.of(
                "text", "Xin chào",
                "targetLanguages", List.of("en", "ja")
        );

        mvc.perform(post("/api/ai/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses.ja").value("FAILED"))
                .andExpect(jsonPath("$.errors.ja").value("AWS timed out"));
    }
}
