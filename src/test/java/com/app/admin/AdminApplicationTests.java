package com.app.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdminApplication.class, properties = {
		"media.storage.provider=azure",
		"media.storage.azure.connection-string=UseDevelopmentStorage=true",
		"media.public-base-url=https://example.invalid/public-media",
		"app.admin.jwt-secret=application-test-secret-at-least-32-bytes-long",
		"spring.datasource.url=jdbc:h2:mem:adminapp;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=classpath:admin-security-test-schema.sql",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		"media.storage.azure.connection-string=UseDevelopmentStorage=true",
		"media.public-base-url=https://example.invalid/public-media"
})
class AdminApplicationTests {
	@Autowired JdbcTemplate jdbc;

	@Test
	void contextLoads() {
		org.junit.jupiter.api.Assertions.assertEquals(0,
				jdbc.queryForObject("SELECT COUNT(*) FROM admin_users", Integer.class));
	}

}
