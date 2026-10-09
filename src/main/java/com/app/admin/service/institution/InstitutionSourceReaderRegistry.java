package com.app.admin.service.institution;

import java.util.List;
import org.springframework.stereotype.Component;

import com.app.admin.exception.InstitutionException;
import com.app.admin.model.InstitutionSource;

@Component
public class InstitutionSourceReaderRegistry {

    private final List<InstitutionSourceReader> readers;

    public InstitutionSourceReaderRegistry(List<InstitutionSourceReader> readers) {
        this.readers = readers;
    }

    public InstitutionSourceReader getReader(InstitutionSource source) {
        // First try to find a source-specific reader (not generic)
        for (InstitutionSourceReader reader : readers) {
            if (!(reader instanceof GenericCsvInstitutionSourceReader) && reader.supports(source)) {
                return reader;
            }
        }
        // Fallback to generic reader
        for (InstitutionSourceReader reader : readers) {
            if (reader.supports(source)) {
                return reader;
            }
        }
        throw InstitutionException.fileNotSupported("No source reader found supporting source: " + (source != null ? source.getCode() : "null"));
    }
}
