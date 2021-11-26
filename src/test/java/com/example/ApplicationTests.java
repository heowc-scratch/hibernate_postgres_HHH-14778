package com.example;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.LongType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Transactional
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationTests {

    @Autowired
    private EntityManager entityManager;

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
    void nativeQuery() {
        assertThatThrownBy(() -> {
            final Long id = 1L;
            final Query query =
                    entityManager.createNativeQuery("UPDATE message SET count = :count WHERE id = :id")
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

    @Test
    void solvedNativeQuery() {
        final Long id = 1L;
        final Query query =
                entityManager.createNativeQuery("UPDATE message SET count = :count WHERE id = :id")
                             .setParameter("count", new TypedParameterValue(LongType.INSTANCE, null))
                             .setParameter("id", id);
        query.executeUpdate();
    }

    @Test
    void namedQuery() {
        final Long id = 1L;
        // UPDATE Message m SET m.count = :count WHERE m.id = :id
        final Query query = entityManager.createNamedQuery("fixedCount")
                                         .setParameter("count", null)
                                         .setParameter("id", id);
        query.executeUpdate();
    }
}
