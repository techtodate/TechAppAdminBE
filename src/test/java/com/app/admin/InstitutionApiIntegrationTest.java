package com.app.admin;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;
import com.app.admin.model.*;
import com.app.admin.repository.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.admin.jwt-secret=integration-test-secret-at-least-32-bytes-long",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:admin-security-test-schema.sql",
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
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    private final HttpClient client = HttpClient.newHttpClient();
    private String adminToken;

    private HttpResponse<String> request(String method, String path, String body, String type) throws Exception {
        return requestWithToken(method, path, body, type, adminToken);
    }
    private HttpResponse<String> requestWithToken(String method, String path, String body, String type, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/admin" + path)).timeout(java.time.Duration.ofSeconds(30));
        if (type != null) builder.header("Content-Type", type);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @SuppressWarnings("unchecked") private Map<String, Object> object(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body()); return json.readValue(response.body(), Map.class);
    }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> array(HttpResponse<String> response) {
        assertEquals(200, response.statusCode(), response.body()); return json.readValue(response.body(), List.class);
    }
    private long number(Map<String, Object> data, String key) { return ((Number) data.get(key)).longValue(); }

    @BeforeEach void createSuperAdminAndAuthenticate() throws Exception {
        jdbc.update("DELETE FROM admin_audit_logs");
        jdbc.update("DELETE FROM admin_refresh_tokens");
        jdbc.update("DELETE FROM admin_user_role_mapping");
        jdbc.update("DELETE FROM admin_role_permission_mapping");
        jdbc.update("DELETE FROM admin_roles");
        jdbc.update("DELETE FROM admin_permissions");
        jdbc.update("DELETE FROM admin_users");
        jdbc.update("INSERT INTO admin_roles(id,code,name,is_system_role,is_active) VALUES (1,'SUPER_ADMIN','SUPER_ADMIN',TRUE,TRUE)");
        jdbc.update("INSERT INTO admin_users(id,username,email,password_hash,first_name,is_active,must_change_password) VALUES (10001,'integration-admin','integration-admin@example.test',?,'Integration',TRUE,FALSE)", passwords.encode("TestAdminPassword123!"));
        jdbc.update("INSERT INTO admin_user_role_mapping(admin_user_id,admin_role_id) VALUES (10001,1)");
        var response = requestWithToken("POST", "/auth/login", "{\"username\":\"integration-admin\",\"password\":\"TestAdminPassword123!\"}", "application/json", null);
        assertEquals(200, response.statusCode(), response.body());
        adminToken = String.valueOf(json.readValue(response.body(), Map.class).get("access_token"));
    }

    @Test void adminLoginRefreshRotationAndAuthorizationAreEnforced() throws Exception {
        var unauthenticated = requestWithToken("GET", "/institution-countries", null, null, null);
        assertEquals(401, unauthenticated.statusCode());

        var loginFailure = requestWithToken("POST", "/auth/login", "{\"username\":\"integration-admin\",\"password\":\"wrong-password\"}", "application/json", null);
        assertEquals(401, loginFailure.statusCode());
        var unknownUser = requestWithToken("POST", "/auth/login", "{\"email\":\"missing@example.test\",\"password\":\"TestAdminPassword123!\"}", "application/json", null);
        assertEquals(401, unknownUser.statusCode());
        jdbc.update("UPDATE admin_users SET is_active=FALSE WHERE id=10001");
        var disabledLogin = requestWithToken("POST", "/auth/login", "{\"username\":\"integration-admin\",\"password\":\"TestAdminPassword123!\"}", "application/json", null);
        assertEquals(401, disabledLogin.statusCode());
        jdbc.update("UPDATE admin_users SET is_active=TRUE WHERE id=10001");
        assertEquals(401, requestWithToken("GET", "/institution-countries", null, null, mainApplicationToken()).statusCode());

        jdbc.update("INSERT INTO admin_roles(id,code,name,is_system_role,is_active) VALUES (2,'LIMITED','LIMITED',FALSE,TRUE)");
        jdbc.update("INSERT INTO admin_users(id,username,email,password_hash,first_name,is_active,must_change_password) VALUES (10002,'limited','limited@example.test',?,'Limited',TRUE,FALSE)", passwords.encode("TestAdminPassword123!"));
        jdbc.update("INSERT INTO admin_user_role_mapping(admin_user_id,admin_role_id) VALUES (10002,2)");
        var limitedLogin = requestWithToken("POST", "/auth/login", "{\"email\":\"limited@example.test\",\"password\":\"TestAdminPassword123!\"}", "application/json", null);
        String limitedToken = String.valueOf(json.readValue(limitedLogin.body(), Map.class).get("access_token"));
        assertEquals(403, requestWithToken("GET", "/institution-countries", null, null, limitedToken).statusCode());

        var login = requestWithToken("POST", "/auth/login", "{\"username\":\"integration-admin\",\"password\":\"TestAdminPassword123!\"}", "application/json", null);
        Map<String, Object> tokenPair = json.readValue(login.body(), Map.class);
        String refreshToken = String.valueOf(tokenPair.get("refresh_token"));
        var refreshed = requestWithToken("POST", "/auth/refresh", "{\"refresh_token\":\"" + refreshToken + "\"}", "application/json", null);
        assertEquals(200, refreshed.statusCode(), refreshed.body());
        assertNotEquals(refreshToken, json.readValue(refreshed.body(), Map.class).get("refresh_token"));
        assertEquals(401, requestWithToken("POST", "/auth/refresh", "{\"refresh_token\":\"" + refreshToken + "\"}", "application/json", null).statusCode());
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_logs WHERE action='TOKEN_REFRESH'", Integer.class) > 0);

        var logoutLogin = requestWithToken("POST", "/auth/login", "{\"username\":\"integration-admin\",\"password\":\"TestAdminPassword123!\"}", "application/json", null);
        Map<String, Object> logoutPair = json.readValue(logoutLogin.body(), Map.class);
        var logout = requestWithToken("POST", "/auth/logout", "{\"refresh_token\":\"" + logoutPair.get("refresh_token") + "\"}", "application/json", adminToken);
        assertEquals(204, logout.statusCode());
        assertEquals(401, requestWithToken("POST", "/auth/refresh", "{\"refresh_token\":\"" + logoutPair.get("refresh_token") + "\"}", "application/json", null).statusCode());
    }

    private String mainApplicationToken() throws Exception {
        String key = "integration-test-secret-at-least-32-bytes-long";
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        long now = java.time.Instant.now().getEpochSecond();
        String claims = "{\"iss\":\"zeto9-main-app\",\"aud\":\"main-app-api\",\"token_use\":\"access\",\"sub\":\"10001\",\"iat\":"+now+",\"exp\":"+(now+600)+",\"jti\":\"main-token-test\"}";
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.US_ASCII)));
    }

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
