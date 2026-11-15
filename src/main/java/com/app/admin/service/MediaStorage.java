package com.app.admin.service;

import java.io.InputStream;

public interface MediaStorage {
    String store(String key, InputStream content, long size, String contentType);
    void delete(String key);
    String publicUrl(String key);
    default String shortLivedDownloadUrl(String key) {
        throw new UnsupportedOperationException("Short-lived download URLs are not supported by this storage provider");
    }
    default String shortLivedDownloadUrl(String key, String container) {
        return shortLivedDownloadUrl(key);
    }
    default String shortLivedViewUrl(String key) {
        return shortLivedDownloadUrl(key);
    }
    default String shortLivedViewUrl(String key, String container) {
        return shortLivedViewUrl(key);
    }
}
