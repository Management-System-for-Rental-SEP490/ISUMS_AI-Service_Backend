package com.isums.aiservice.infrastructures.clients;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Component
@RequiredArgsConstructor
public class S3StoreClient {

    private final S3Client s3;

    public byte[] getBytes(String bucket, String key) {
        ResponseBytes<?> bytes = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toBytes());
        return bytes.asByteArray();
    }
}
