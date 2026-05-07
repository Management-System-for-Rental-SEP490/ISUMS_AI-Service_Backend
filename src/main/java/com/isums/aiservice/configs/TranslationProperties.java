package com.isums.aiservice.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.translation")
public class TranslationProperties {

    private List<String> terminologyNames = new ArrayList<>();
    private CustomerFacing customerFacing = new CustomerFacing();

    @Getter
    @Setter
    public static class CustomerFacing {
        private boolean bedrockEnabled = false;
        private String bedrockModelId = "amazon.nova-lite-v1:0";
        private String bedrockRegion = "us-east-1";
        private Integer bedrockMaxTokens = 220;
        private Float bedrockTemperature = 0.0F;
    }
}
