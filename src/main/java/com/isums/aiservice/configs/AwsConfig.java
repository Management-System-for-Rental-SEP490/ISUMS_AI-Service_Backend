package com.isums.aiservice.configs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.translate.TranslateClient;

@Configuration
@EnableConfigurationProperties(TranslationProperties.class)
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder().region(Region.AP_SOUTHEAST_1).build();
    }

    @Bean
    public TranslateClient translateClient() {
        return TranslateClient.builder().region(Region.AP_SOUTHEAST_1).build();
    }

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(TranslationProperties translationProperties) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(translationProperties.getCustomerFacing().getBedrockRegion()))
                .build();
    }
}
