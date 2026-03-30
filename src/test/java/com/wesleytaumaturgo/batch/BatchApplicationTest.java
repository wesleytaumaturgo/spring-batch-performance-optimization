package com.wesleytaumaturgo.batch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifica que o contexto Spring carrega sem erros.
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchApplicationTest {

    @Test
    void should_load_spring_context_without_errors() {
        // Contexto carregado com sucesso se este teste passar
    }
}
