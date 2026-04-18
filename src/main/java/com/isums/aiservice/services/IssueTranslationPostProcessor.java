package com.isums.aiservice.services;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class IssueTranslationPostProcessor {

    public String refine(String originalText, String translatedText, String sourceLanguage, String targetLanguage) {
        if (originalText == null || originalText.isBlank()) {
            return translatedText;
        }

        String normalizedSource = normalizeVietnamese(originalText);
        String normalizedTarget = targetLanguage == null ? null : targetLanguage.toLowerCase(Locale.ROOT);

        if (normalizedSource.contains("da kiem tra")
                && normalizedSource.contains("vui long xac nhan")
                && normalizedSource.contains("tien hanh sua chua")) {
            if ("en".equals(normalizedTarget)) {
                return "I have checked it. Please confirm so we can proceed with the repair.";
            }
            if ("ja".equals(normalizedTarget)) {
                return "確認いたしました。修理を進めるため、ご確認をお願いいたします。";
            }
        }

        if (normalizedSource.contains("vui long xac nhan")
                && normalizedSource.contains("tien hanh sua chua")) {
            if ("en".equals(normalizedTarget)) {
                return "Please confirm so we can proceed with the repair.";
            }
            if ("ja".equals(normalizedTarget)) {
                return "修理を進めるため、ご確認をお願いいたします。";
            }
        }

        if (normalizedSource.contains("vui long xac nhan")) {
            if ("en".equals(normalizedTarget)) {
                return "Please confirm.";
            }
            if ("ja".equals(normalizedTarget)) {
                return "ご確認をお願いいたします。";
            }
        }

        return translatedText;
    }

    private String normalizeVietnamese(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT);
    }
}
