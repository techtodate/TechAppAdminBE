package com.app.admin.service;

import java.io.InputStream;

public interface MediaStorage {
    String store(String key, InputStream content, long size, String contentType);
    void delete(String key);
    String publicUrl(String key);
}
