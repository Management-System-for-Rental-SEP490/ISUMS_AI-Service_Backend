package com.isums.aiservice.services;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TranslationLocaleSupport {

    public String normalize(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        return Locale.forLanguageTag(locale.replace('_', '-').trim()).getLanguage();
    }
}
