package com.example;

import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationTests {

    @Autowired
    private EntityManager entityManager;
    @Container
    private final GenericContainer<?> postgresql = new PostgreSQLContainer<>("postgres:14.1-alpine")
            .withDatabaseName("public")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeEach
    void beforeEach() {
        final Message message = Message.builder()
                                       .body("message")
                                       .count(0L)
                                       .build();

        entityManager.persist(message);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void test() {
        assertThatThrownBy(() -> {
            final Long id = 1L;
            final Query query = entityManager.createNativeQuery("UPDATE message SET count = :count WHERE id = :id")
                                             .setParameter("count", null)
                                             .setParameter("id", id);
            query.executeUpdate();
        })
        .hasCauseInstanceOf(SQLGrammarException.class)
        .hasRootCauseMessage(
                "ERROR: column \"count\" is of type bigint but expression is of type bytea\n" +
                "  Hint: You will need to rewrite or cast the expression.\n" +
                "  Position: 28");
    }
}
