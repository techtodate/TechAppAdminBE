package com.app.admin.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " was not found");
    }

    public ResourceNotFoundException(String resource, String key) {
        super(resource + " '" + key + "' was not found");
    }
}
