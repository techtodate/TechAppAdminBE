package com.app.admin;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;
import com.app.admin.model.*;
import com.app.admin.repository.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:institutions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false", "spring.jackson.property-naming-strategy=SNAKE_CASE",
        "media.storage.provider=azure", "media.storage.azure.connection-string=UseDevelopmentStorage=true",
        "media.public-base-url=https://example.invalid/public-media"
})
class InstitutionApiIntegrationTest {
    @TempDir static Path uploads;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.institution-import.storage-directory", () -> uploads.toString());
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired CountryRepository countries;
    @Autowired InstitutionSourceRepository sources;
    @Autowired InstitutionStandardFieldRepository fields;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> request(String method, String path, String body, String type) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/admin" + path)).timeout(java.time.Duration.ofSeconds(30));
        if (type != null) builder.header("Content-Type", type);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @SuppressWarnings("unchecked") private Map<String, Object> object(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body()); return json.readValue(response.body(), Map.class);
    }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> array(HttpResponse<String> response) {
        assertEquals(200, response.statusCode(), response.body()); return json.readValue(response.body(), List.class);
    }
    private long number(Map<String, Object> data, String key) { return ((Number) data.get(key)).longValue(); }

    @Test void realHttpContractSupportsDashboardMappingUploadValidationApplyAndFilters() throws Exception {
        Country country = new Country(); country.setIsoCode("ZZ"); country.setName("Contract Country"); country = countries.saveAndFlush(country);
        InstitutionSource source = new InstitutionSource(); source.setCountryId(country.getId()); source.setSourceName("Contract Source"); source.setSourceCode("CONTRACT"); source = sources.saveAndFlush(source);
        for (String code : List.of("SOURCE_IDENTIFIER", "NAME")) { InstitutionStandardField field = new InstitutionStandardField(); field.setFieldCode(code); field.setFieldName(code); field.setIsRequired(true); fields.saveAndFlush(field); }
        assertFalse(array(request("GET", "/institution-countries", null, null)).isEmpty());
        var sourceRows = array(request("GET", "/institution-sources?countryId=" + country.getId(), null, null));
        assertEquals("Contract Source", sourceRows.get(0).get("source_name"));
        assertTrue(sourceRows.get(0).get("country") instanceof Map);
        array(request("GET", "/institution-standard-fields", null, null));
        var dashboard = object(request("GET", "/institutions/summary", null, null), 200);
        assertEquals(0, number(dashboard, "total_institutions"));
        String mapping = "[{\"source_field_name\":\"code\",\"standard_field_code\":\"SOURCE_IDENTIFIER\",\"transformation_rule\":\"TRIM\"},{\"source_field_name\":\"name\",\"standard_field_code\":\"NAME\",\"transformation_rule\":\"NORMALIZE_NAME\"}]";
        array(request("PUT", "/institution-source-mappings?sourceId=" + source.getId(), mapping, "application/json"));
        var saved = array(request("GET", "/institution-source-mappings?sourceId=" + source.getId(), null, null));
        assertEquals("SOURCE_IDENTIFIER", saved.get(0).get("standard_field_code"));
        String payload = "{\"country_id\":" + country.getId() + ",\"source_id\":" + source.getId() + ",\"version\":\"contract-1\",\"import_type\":\"FULL\"}";
        var created = object(request("POST", "/institution-imports", payload, "application/json"), 201);
        long id = number(created, "id"); assertEquals("CREATED", created.get("status"));
        assertEquals(400, request("POST", "/institution-imports", "{}", "application/json").statusCode());
        String boundary = "contractboundary";
        String multipart = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"contract.csv\"\r\nContent-Type: text/csv\r\n\r\ncode,name\nA1,Alpha Contract College\nB2,Beta Contract College\n\r\n--" + boundary + "--\r\n";
        assertEquals("UPLOADED", object(request("POST", "/institution-imports/" + id + "/upload", multipart, "multipart/form-data; boundary=" + boundary), 200).get("status"));
        assertTrue(java.nio.file.Files.exists(uploads.resolve(id + "/source.csv")));
        for (int i = 0; i < 2; i++) assertEquals("READY_FOR_REVIEW", object(request("POST", "/institution-imports/" + id + "/validate", null, null), 200).get("status"));
        var rows = object(request("GET", "/institution-imports/" + id + "/records?classification=NEW&page=0&size=1", null, null), 200);
        assertEquals(2, number(rows, "total_elements"));
        assertEquals(1, ((List<?>) rows.get("content")).size());
        assertEquals("COMPLETED", object(request("POST", "/institution-imports/" + id + "/apply", null, null), 200).get("status"));
        assertEquals(409, request("POST", "/institution-imports/" + id + "/apply", null, null).statusCode());
        var list = object(request("GET", "/institutions/search?q=Alpha&countryId=" + country.getId() + "&active=true&verificationStatus=VERIFIED&page=0&size=20&sort=name,asc", null, null), 200);
        assertEquals(1, number(list, "total_elements"));
        long institutionId = ((Number) ((Map<?, ?>) ((List<?>) list.get("content")).get(0)).get("id")).longValue();
        var detail = object(request("GET", "/institutions/" + institutionId, null, null), 200);
        assertEquals(1, ((List<?>) detail.get("source_records")).size()); assertNotNull(detail.get("source"));
        assertEquals(2, number(object(request("GET", "/institutions/summary", null, null), 200), "total_institutions"));
        assertEquals(1, number(object(request("GET", "/institution-import-history?countryId=" + country.getId() + "&sourceId=" + source.getId() + "&status=COMPLETED", null, null), 200), "total_elements"));
        assertEquals(0, number(object(request("GET", "/institution-import-history?status=CANCELLED", null, null), 200), "total_elements"));
    }
}
