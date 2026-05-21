package com.example.demo;

import com.ruc.platform.PlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = PlatformApplication.class)
@ActiveProfiles("h2")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
