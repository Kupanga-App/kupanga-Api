package com.kupanga.api;

import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@MockBean(UtilisateurRepository.class)
@ActiveProfiles("test")
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
