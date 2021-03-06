package com.example;

import org.hibernate.Session;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.LongType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationTests {

    @Autowired
    private EntityManager entityManager;

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
    void solvedNativeQueryUsingTypeParameterValue() {
        final Long id = 1L;
        final Query query =
                entityManager.createNativeQuery("UPDATE message SET count = :count WHERE id = :id")
                             .setParameter("count", new TypedParameterValue(LongType.INSTANCE, null))
                             .setParameter("id", id);
        query.executeUpdate();
    }

    @Test
    void solvedNativeQueryUsingTwiceCasting() {
        final Long id = 1L;
        final Query query =
                entityManager.createNativeQuery("UPDATE message SET count = cast(cast(:count as text) as bigint) WHERE id = :id")
                             .setParameter("count", null)
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


    @Test
    void prepareStatement() throws SQLException {
        final Session session = entityManager.unwrap(Session.class);
        final Long id = 1L;
        assertThatThrownBy(() -> {
            session.doWork(connection -> {
                try (final PreparedStatement ps = connection.prepareStatement("UPDATE message SET count = ? WHERE id = ?")) {
                    ps.setNull(1, Types.BINARY);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
            });
        })
        .hasRootCauseMessage(
                "ERROR: column \"count\" is of type bigint but expression is of type bytea\n" +
                "  Hint: You will need to rewrite or cast the expression.\n" +
                "  Position: 28");
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void solvedPrepareStatement(Consumer<PreparedStatement> consumer)
            throws SQLException {
        final Session session = entityManager.unwrap(Session.class);
        final Long id = 1L;
        session.doWork(connection -> {
            try (final PreparedStatement ps = connection.prepareStatement("UPDATE message SET count = ? WHERE id = ?")) {
                consumer.accept(ps);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
        });
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
            Arguments.of((Consumer<PreparedStatement>) ps -> {
                try {
                    ps.setNull(1, Types.INTEGER);
                } catch (SQLException e) {
                    // ignored
                }
            }),
            Arguments.of((Consumer<PreparedStatement>) ps -> {
                try {
                    ps.setNull(1, Types.BINARY, "int8");
                } catch (SQLException e) {
                    // ignored
                }
            }),
            Arguments.of((Consumer<PreparedStatement>) ps -> {
                try {
                    ps.setNull(1, Types.BINARY, "null"); // Not recommend
                } catch (SQLException e) {
                    // ignored
                }
            })
        );
    }

    // See https://github.com/spring-projects/spring-data-jpa/issues/2370
    // See https://github.com/spring-projects/spring-data-jpa/pull/2461
    @Test
    void dataJpa(@Autowired MessageRepository repository) {
        repository.update(null, 1L);
    }
}
