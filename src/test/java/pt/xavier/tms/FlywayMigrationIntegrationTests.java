package pt.xavier.tms;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tms_test")
            .withUsername("tms_test")
            .withPassword("tms_test");

    @Test
    void appliesBasePostgresExtensionsMigration() throws SQLException {
        migrate();

        assertThat(installedExtensions()).contains("uuid-ossp", "pg_trgm");
    }

    @Test
    void createsSpringModulithEventPublicationTable() throws SQLException {
        migrate();

        assertThat(tableColumns("event_publication"))
                .containsEntry("id", "uuid")
                .containsEntry("completion_date", "timestamp with time zone(6)")
                .containsEntry("event_type", "character varying(255)")
                .containsEntry("listener_id", "character varying(255)")
                .containsEntry("publication_date", "timestamp with time zone(6)")
                .containsEntry("serialized_event", "character varying(255)");

        assertThat(indexNames("event_publication"))
                .contains(
                        "event_publication_pkey",
                        "event_publication_completion_date_publication_date_idx",
                        "event_publication_event_listener_completion_idx"
                );
    }

    private static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static Set<String> installedExtensions() throws SQLException {
        Set<String> extensions = new HashSet<>();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement();
             var resultSet = statement.executeQuery(
                     "select extname from pg_extension where extname in ('uuid-ossp', 'pg_trgm')"
             )) {
            while (resultSet.next()) {
                extensions.add(resultSet.getString("extname"));
            }
        }

        return extensions;
    }

    private static Map<String, String> tableColumns(String tableName) throws SQLException {
        Map<String, String> columns = new HashMap<>();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); PreparedStatement statement = connection.prepareStatement("""
                select column_name,
                       case
                           when character_maximum_length is not null
                               then data_type || '(' || character_maximum_length || ')'
                           when datetime_precision is not null
                               then data_type || '(' || datetime_precision || ')'
                           else data_type
                       end as column_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                """)) {
            statement.setString(1, tableName);

            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    columns.put(resultSet.getString("column_name"), resultSet.getString("column_type"));
                }
            }
        }

        return columns;
    }

    private static Set<String> indexNames(String tableName) throws SQLException {
        Set<String> indexes = new HashSet<>();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); PreparedStatement statement = connection.prepareStatement("""
                select indexname
                from pg_indexes
                where schemaname = 'public'
                  and tablename = ?
                """)) {
            statement.setString(1, tableName);

            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    indexes.add(resultSet.getString("indexname"));
                }
            }
        }

        return indexes;
    }
}
