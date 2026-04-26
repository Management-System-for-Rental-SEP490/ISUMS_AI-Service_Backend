package com.isums.aiservice.infrastructures.kafka;

import com.isums.common.i18n.events.TextTranslationRequestedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TextTranslationTopicsConfig {

    @Bean
    public NewTopic textTranslationRequestedTopic(
            @Value("${ai.translation.kafka.replicas:1}") short replicas,
            @Value("${ai.translation.kafka.request-partitions:3}") int partitions) {
        return TopicBuilder.name(TextTranslationRequestedEvent.TOPIC)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
