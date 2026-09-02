package com.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TopicImageLifecycle {
    private static final List<String> VARIANTS = List.of("thumb", "small", "medium");

    private final MediaStorage storage;

    public TopicImageLifecycle(MediaStorage storage) {
        this.storage = storage;
    }

    public void deleteAfterCommit(String preferredKey) {
        if (preferredKey == null || preferredKey.isBlank()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteNow(preferredKey);
            }
        });
    }

    public void deleteNow(String preferredKey) {
        for (String key : keysForImage(preferredKey)) storage.delete(key);
    }

    public List<String> keysForImage(String preferredKey) {
        String normalized = preferredKey.replaceFirst("^/+", "");
        if (!normalized.endsWith("/small.webp")) return List.of(normalized);
        String prefix = normalized.substring(0, normalized.length() - "small.webp".length());
        return VARIANTS.stream().map(name -> prefix + name + ".webp").toList();
    }
}
