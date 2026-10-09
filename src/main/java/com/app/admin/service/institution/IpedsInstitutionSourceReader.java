package com.app.admin.service.institution;

import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Component;

import com.app.admin.dto.SourceFileMetadata;
import com.app.admin.dto.SourceRow;
import com.app.admin.exception.InstitutionException;
import com.app.admin.model.InstitutionSource;

@Component
public class IpedsInstitutionSourceReader implements InstitutionSourceReader {

    @Override
    public boolean supports(InstitutionSource source) {
        return source != null && "IPEDS".equalsIgnoreCase(source.getCode());
    }

    @Override
    public List<SourceRow> read(InputStream inputStream, SourceFileMetadata metadata) throws Exception {
        if (metadata != null && metadata.fileName() != null && !metadata.fileName().toLowerCase().endsWith(".csv")) {
            throw InstitutionException.fileNotSupported("IPEDS reader only supports CSV files currently. Provided: " + metadata.fileName());
        }
        return CsvHelper.parseCsv(inputStream);
    }
}
