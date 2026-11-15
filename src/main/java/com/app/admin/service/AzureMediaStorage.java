package com.app.admin.service;

import java.io.InputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import java.time.OffsetDateTime;

@Service
@ConditionalOnProperty(name = "media.storage.provider", havingValue = "azure")
public class AzureMediaStorage implements MediaStorage {
    private final BlobContainerClient container;
    private final String connectionString;
    private final String containerName;
    private final String publicBaseUrl;

    public AzureMediaStorage(
            @Value("${media.storage.azure.connection-string}") String connectionString,
            @Value("${media.storage.azure.container}") String containerName,
            @Value("${media.public-base-url}") String publicBaseUrl) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException("AZURE_STORAGE_CONNECTION_STRING must contain a valid Azure Blob Storage connection string");
        }
        if (containerName == null || containerName.isBlank()) {
            throw new IllegalStateException("AZURE_STORAGE_CONTAINER must name the media container");
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("AZURE_STORAGE_PUBLIC_BASE_URL must contain the public media base URL");
        }
        this.connectionString = connectionString;
        this.containerName = containerName;
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

    @Override
    public String shortLivedDownloadUrl(String key) {
        return shortLivedUrl(key, containerName, "attachment");
    }

    @Override
    public String shortLivedDownloadUrl(String key, String container) {
        return shortLivedUrl(key, container, "attachment");
    }

    @Override
    public String shortLivedViewUrl(String key) {
        return shortLivedUrl(key, containerName, "inline");
    }

    @Override
    public String shortLivedViewUrl(String key, String container) {
        return shortLivedUrl(key, container, "inline");
    }

    private String shortLivedUrl(String key, String requestedContainer, String disposition) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Storage key is required");
        String effectiveContainer = requestedContainer == null || requestedContainer.isBlank() ? containerName : requestedContainer;
        var blob = effectiveContainer.equals(containerName) ? container.getBlobClient(key)
                : new BlobContainerClientBuilder().connectionString(connectionString).containerName(effectiveContainer).buildClient().getBlobClient(key);
        var permissions = new BlobSasPermission().setReadPermission(true);
        var values = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusMinutes(10), permissions)
                .setContentDisposition(disposition);
        return blob.getBlobUrl() + "?" + blob.generateSas(values);
    }
}
