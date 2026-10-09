package com.app.admin.service.institution;

import java.io.InputStream;
import java.util.List;
import com.app.admin.dto.SourceFileMetadata;
import com.app.admin.dto.SourceRow;
import com.app.admin.model.InstitutionSource;

public interface InstitutionSourceReader {
    boolean supports(InstitutionSource source);
    List<SourceRow> read(InputStream inputStream, SourceFileMetadata metadata) throws Exception;
}
