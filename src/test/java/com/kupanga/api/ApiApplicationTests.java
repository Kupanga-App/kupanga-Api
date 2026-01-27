package com.kupanga.api;

import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@MockBean(UserRepository.class)
@MockBean(RefreshToken.class)
@ActiveProfiles("test")
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
