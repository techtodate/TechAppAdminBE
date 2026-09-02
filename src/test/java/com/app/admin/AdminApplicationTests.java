package com.app.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdminApplication.class, properties = {
		"media.storage.provider=azure",
		"media.storage.azure.connection-string=UseDevelopmentStorage=true",
		"media.public-base-url=https://example.invalid/public-media"
})
class AdminApplicationTests {

	@Test
	void contextLoads() {
	}

}
