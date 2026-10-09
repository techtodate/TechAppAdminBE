package com.app.admin.exception;

import org.springframework.http.HttpStatus;

public class InstitutionException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public InstitutionException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static InstitutionException sourceNotFound(Long id) {
        return new InstitutionException(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", "Institution source not found with id: " + id);
    }

    public static InstitutionException countryNotFound(Long id) {
        return new InstitutionException(HttpStatus.NOT_FOUND, "COUNTRY_NOT_FOUND", "Country not found with id: " + id);
    }

    public static InstitutionException invalidSourceCountry(Long sourceId, Long countryId) {
        return new InstitutionException(HttpStatus.BAD_REQUEST, "INVALID_SOURCE_COUNTRY",
                "Source ID " + sourceId + " does not belong to country ID " + countryId);
    }

    public static InstitutionException fileNotSupported(String message) {
        return new InstitutionException(HttpStatus.BAD_REQUEST, "FILE_NOT_SUPPORTED", message);
    }

    public static InstitutionException fileEmpty() {
        return new InstitutionException(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "Uploaded file is empty");
    }

    public static InstitutionException mappingNotFound(Long sourceId) {
        return new InstitutionException(HttpStatus.BAD_REQUEST, "MAPPING_NOT_FOUND",
                "No field mappings found for institution source ID " + sourceId);
    }

    public static InstitutionException requiredFieldMissing(String fieldName) {
        return new InstitutionException(HttpStatus.BAD_REQUEST, "REQUIRED_FIELD_MISSING",
                "Required field mapping or value missing: " + fieldName);
    }

    public static InstitutionException invalidLocation(String message) {
        return new InstitutionException(HttpStatus.BAD_REQUEST, "INVALID_LOCATION", message);
    }

    public static InstitutionException duplicateSourceIdentifier(String identifier) {
        return new InstitutionException(HttpStatus.CONFLICT, "DUPLICATE_SOURCE_IDENTIFIER",
                "Duplicate source identifier found: " + identifier);
    }

    public static InstitutionException importNotReady(String currentStatus) {
        return new InstitutionException(HttpStatus.CONFLICT, "IMPORT_NOT_READY",
                "Import is not in ready state for this action. Current status: " + currentStatus);
    }

    public static InstitutionException importAlreadyApplied(Long importId) {
        return new InstitutionException(HttpStatus.CONFLICT, "IMPORT_ALREADY_APPLIED",
                "Import ID " + importId + " has already been applied");
    }

    public static InstitutionException importAlreadyCancelled(Long importId) {
        return new InstitutionException(HttpStatus.CONFLICT, "IMPORT_ALREADY_CANCELLED",
                "Import ID " + importId + " has already been cancelled");
    }

    public static InstitutionException unauthorizedImportAction(String message) {
        return new InstitutionException(HttpStatus.FORBIDDEN, "UNAUTHORIZED_IMPORT_ACTION", message);
    }
}
