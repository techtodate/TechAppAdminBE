package com.app.admin;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;
import com.app.admin.dto.*;
import com.app.admin.model.*;
import com.app.admin.repository.*;
import com.app.admin.service.institution.*;

/** Explicitly enabled local PostgreSQL contract test. All fixture rows roll back. */
@EnabledIfEnvironmentVariable(named="INSTITUTION_POSTGRES_AUDIT", matches="true")
@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false"})
@Transactional
class InstitutionPostgresContractTest {
    @TempDir static Path uploads;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.institution-import.storage-directory", () -> uploads.toString());
    }
    @Autowired EntityManager em;
    @Autowired CountryRepository countries;
    @Autowired InstitutionSourceRepository sources;
    @Autowired InstitutionStandardFieldRepository fields;
    @Autowired InstitutionSourceMappingRepository mappings;
    @Autowired InstitutionImportService workflow;
    @Autowired InstitutionApiService api;
    @Autowired InstitutionImportRecordRepository records;
    @Autowired InstitutionAliasRepository aliases;

    private long prepare(long country, long source, String csv) {
        var item=workflow.createImport(new CreateInstitutionImportRequest(country, source, "audit", "FULL", null));
        long id=item.getId();
        workflow.uploadSourceFile(id, new MockMultipartFile("file", "audit.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)));
        workflow.validateImport(id); em.flush(); em.clear();
        return id;
    }

    @Test void actualSchemaSupportsImportUpdateUnchangedDuplicatesCancellationAndQueries() {
        // Also exercises every mapped institution table against PostgreSQL, not generated DDL.
        for (Class<?> type : List.of(Country.class, AdministrativeArea.class, City.class,
                Institution.class, InstitutionSource.class, InstitutionStandardField.class,
                InstitutionSourceMapping.class, InstitutionImport.class, InstitutionImportRecord.class,
                InstitutionImportRecordMatch.class, InstitutionAlias.class, InstitutionSourceRecord.class)) {
            em.createQuery("from " + type.getSimpleName()).setMaxResults(1).getResultList();
        }
        Country country=new Country(); country.setIsoCode("QA"+UUID.randomUUID().toString().substring(0,6));
        country.setName("Audit " + UUID.randomUUID()); country=countries.saveAndFlush(country);
        InstitutionSource source=new InstitutionSource(); source.setCountryId(country.getId());
        source.setSourceCode("AUDIT"); source.setSourceName("Rollback audit source"); source=sources.saveAndFlush(source);
        for(String code:List.of("SOURCE_IDENTIFIER","NAME","SHORT_NAME","WEBSITE")) {
            if(fields.findByCode(code).isEmpty()) {var f=new InstitutionStandardField(); f.setFieldCode(code); f.setFieldName(code); fields.saveAndFlush(f);}
            var m=new InstitutionSourceMapping(); m.setSourceId(source.getId()); m.setSourceFieldName(code); m.setStandardFieldCode(code); mappings.saveAndFlush(m);
        }
        long cid=country.getId(), sid=source.getId();
        String header="SOURCE_IDENTIFIER,NAME,SHORT_NAME,WEBSITE\n";
        String row="A1,Audit University,AU,https://audit.example\n";
        long first=prepare(cid,sid,header+row);
        assertEquals(1, workflow.getImport(first).getValidRecords());
        workflow.applyImport(first); em.flush(); em.clear();
        assertNotNull(workflow.getImport(first).getCompletedAt());
        assertNotNull(workflow.getImport(first).getUpdatedAt());
        assertFalse(aliases.findByNormalizedAlias("au").isEmpty());
        long repeat=prepare(cid,sid,header+row);
        assertEquals(RecordClassification.UNCHANGED, records.findByImportIdOrderByRowNumberAsc(repeat).get(0).getClassification());
        workflow.applyImport(repeat); em.flush(); em.clear();
        long update=prepare(cid,sid,header+row.replace("Audit University", "Audit Updated University"));
        assertEquals(RecordClassification.UPDATE, records.findByImportIdOrderByRowNumberAsc(update).get(0).getClassification());
        workflow.applyImport(update); em.flush(); em.clear();
        long duplicate=prepare(cid,sid,header+"B1,Audit Updated University,AU,https://audit.example\n");
        var matches=workflow.getImportDuplicates(duplicate); assertFalse(matches.isEmpty());
        workflow.mergeDuplicate(duplicate,matches.get(0).getId());
        workflow.applyImport(duplicate); em.flush(); em.clear();
        long separate=prepare(cid,sid,header+"C1,Audit Updated University,AU,https://audit.example\n");
        var candidates=workflow.getImportDuplicates(separate); assertFalse(candidates.isEmpty());
        workflow.keepDuplicateSeparate(separate,candidates.get(0).getId());
        workflow.applyImport(separate); em.flush(); em.clear();
        long cancelled=workflow.createImport(new CreateInstitutionImportRequest(cid,sid,"cancel","INCREMENTAL",null)).getId();
        workflow.cancelImport(cancelled); em.flush(); em.clear();
        assertEquals(ImportStatus.CANCELLED, workflow.getImport(cancelled).getStatus());
        assertNotNull(api.dashboard()); assertNotNull(api.fields()); assertNotNull(api.sources(cid));
        assertNotNull(api.records(first,null,"Audit",PageRequest.of(0,20)));
        assertNotNull(api.history(cid,sid,"COMPLETED",null,null,PageRequest.of(0,20)));
        assertEquals(2L, api.list(cid,"Audit",null,null,true,PageRequest.of(0,20)).get("total_elements"));
    }
}
