package com.app.admin.service.institution;

import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Component;

import com.app.admin.dto.SourceFileMetadata;
import com.app.admin.dto.SourceRow;
import com.app.admin.model.InstitutionSource;

@Component
public class GenericCsvInstitutionSourceReader implements InstitutionSourceReader {

    @Override
    public boolean supports(InstitutionSource source) {
        // Fallback for any source specifying CSV format
        return source != null && "CSV".equalsIgnoreCase(source.getFileFormat());
    }

    @Override
    public List<SourceRow> read(InputStream inputStream, SourceFileMetadata metadata) throws Exception {
        return CsvHelper.parseCsv(inputStream);
    }
}
