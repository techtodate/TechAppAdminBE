package com.app.admin.service;

import java.io.InputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;

@Service
@ConditionalOnProperty(name = "media.storage.provider", havingValue = "azure")
public class AzureMediaStorage implements MediaStorage {
    private final BlobContainerClient container;
    private final String publicBaseUrl;

    public AzureMediaStorage(
            @Value("${media.storage.azure.connection-string}") String connectionString,
            @Value("${media.storage.azure.container}") String containerName,
            @Value("${media.public-base-url}") String publicBaseUrl) {
        this.container = new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildClient();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public String store(String key, InputStream content, long size, String contentType) {
        var blob = container.getBlobClient(key);
        blob.upload(content, size, true);
        blob.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType)
                .setCacheControl("public, max-age=31536000, immutable"));
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String key) {
        if (key != null && !key.isBlank()) container.getBlobClient(key).deleteIfExists();
    }

    @Override
    public String publicUrl(String key) {
        return key == null || key.isBlank() ? null : publicBaseUrl + "/" + key;
    }
}
