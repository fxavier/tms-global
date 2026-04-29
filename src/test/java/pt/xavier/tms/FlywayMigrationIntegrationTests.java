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

    @Test
    void createsVehicleModuleTablesAndIndexes() throws SQLException {
        migrate();

        assertThat(tableNames()).contains(
                "files",
                "vehicles",
                "vehicle_documents",
                "vehicle_accessories",
                "maintenance_records",
                "checklist_templates",
                "checklist_template_items",
                "checklist_inspections",
                "checklist_inspection_items"
        );

        assertThat(tableColumns("vehicles"))
                .containsEntry("id", "uuid")
                .containsEntry("plate", "character varying(20)")
                .containsEntry("current_driver_id", "uuid")
                .containsEntry("deleted_at", "timestamp with time zone(6)");

        assertThat(tableColumns("vehicle_documents"))
                .containsEntry("vehicle_id", "uuid")
                .containsEntry("file_id", "uuid")
                .containsEntry("deleted_by", "character varying(100)");

        assertThat(indexNames("vehicles"))
                .contains("idx_vehicles_plate", "idx_vehicles_status", "idx_vehicles_plate_trgm");

        assertThat(indexNames("vehicle_documents"))
                .contains("idx_vehicle_docs_expiry", "idx_vehicle_docs_status");

        assertThat(indexNames("vehicle_accessories"))
                .contains("idx_vehicle_accessories_vehicle", "uk_vehicle_accessories_vehicle_type");

        assertThat(indexNames("maintenance_records"))
                .contains("idx_maintenance_vehicle", "idx_maintenance_next_date");

        assertThat(indexNames("checklist_inspections"))
                .contains("idx_checklist_inspections_vehicle");
    }

    @Test
    void createsAuditLogTableAndIndexes() throws SQLException {
        migrate();

        assertThat(tableNames()).contains("audit_logs");

        assertThat(tableColumns("audit_logs"))
                .containsEntry("id", "uuid")
                .containsEntry("entity_type", "character varying(100)")
                .containsEntry("operation", "character varying(30)")
                .containsEntry("performed_by", "character varying(100)")
                .containsEntry("ip_address", "character varying(64)")
                .containsEntry("created_at", "timestamp with time zone(6)")
                .containsEntry("previous_values", "jsonb")
                .containsEntry("new_values", "jsonb");

        assertThat(indexNames("audit_logs"))
                .contains(
                        "idx_audit_entity",
                        "idx_audit_operation",
                        "idx_audit_performed_by",
                        "idx_audit_performed_at"
                );
    }

    @Test
    void createsDriverModuleTablesAndIndexes() throws SQLException {
        migrate();

        assertThat(tableNames()).contains("drivers", "driver_documents");

        assertThat(tableColumns("drivers"))
                .containsEntry("id", "uuid")
                .containsEntry("full_name", "character varying(200)")
                .containsEntry("id_number", "character varying(50)")
                .containsEntry("license_number", "character varying(50)")
                .containsEntry("status", "character varying(30)")
                .containsEntry("deleted_at", "timestamp with time zone(6)");

        assertThat(tableColumns("driver_documents"))
                .containsEntry("driver_id", "uuid")
                .containsEntry("document_type", "character varying(40)")
                .containsEntry("status", "character varying(40)")
                .containsEntry("file_id", "uuid")
                .containsEntry("deleted_by", "character varying(100)");

        assertThat(indexNames("drivers"))
                .contains("idx_drivers_status", "idx_drivers_license_expiry", "idx_drivers_location");

        assertThat(indexNames("driver_documents"))
                .contains("idx_driver_docs_driver", "idx_driver_docs_expiry");
    }

    @Test
    void createsActivityModuleTablesAndIndexes() throws SQLException {
        migrate();

        assertThat(tableNames()).contains("activities", "activity_events");

        assertThat(tableColumns("activities"))
                .containsEntry("id", "uuid")
                .containsEntry("code", "character varying(30)")
                .containsEntry("activity_type", "character varying(100)")
                .containsEntry("priority", "character varying(20)")
                .containsEntry("status", "character varying(20)")
                .containsEntry("vehicle_id", "uuid")
                .containsEntry("driver_id", "uuid")
                .containsEntry("rh_override_justification", "text")
                .containsEntry("deleted_at", "timestamp with time zone(6)");

        assertThat(tableColumns("activity_events"))
                .containsEntry("id", "uuid")
                .containsEntry("activity_id", "uuid")
                .containsEntry("event_type", "character varying(50)")
                .containsEntry("previous_status", "character varying(20)")
                .containsEntry("new_status", "character varying(20)")
                .containsEntry("performed_by", "character varying(100)")
                .containsEntry("performed_at", "timestamp with time zone(6)");

        assertThat(indexNames("activities"))
                .contains(
                        "idx_activities_status",
                        "idx_activities_vehicle",
                        "idx_activities_driver",
                        "idx_activities_planned_start",
                        "idx_activities_code"
                );

        assertThat(indexNames("activity_events"))
                .contains("idx_activity_events_activity");
    }

    @Test
    void createsAlertModuleTablesIndexesAndDefaultConfigurations() throws SQLException {
        migrate();

        assertThat(tableNames()).contains("alerts", "alert_configurations");

        assertThat(tableColumns("alerts"))
                .containsEntry("id", "uuid")
                .containsEntry("alert_type", "character varying(50)")
                .containsEntry("severity", "character varying(20)")
                .containsEntry("entity_type", "character varying(50)")
                .containsEntry("entity_id", "uuid")
                .containsEntry("is_resolved", "boolean")
                .containsEntry("resolved_at", "timestamp with time zone(6)")
                .containsEntry("created_by", "character varying(100)");

        assertThat(indexNames("alerts"))
                .contains("idx_alerts_entity", "idx_alerts_is_resolved", "idx_alerts_severity", "idx_alerts_dedup");

        assertThat(tableColumns("alert_configurations"))
                .containsEntry("id", "uuid")
                .containsEntry("alert_type", "character varying(50)")
                .containsEntry("entity_type", "character varying(50)")
                .containsEntry("days_before_warning", "integer")
                .containsEntry("days_before_critical", "integer")
                .containsEntry("is_active", "boolean");

        assertThat(rowCount("alert_configurations")).isGreaterThanOrEqualTo(3);
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

    private static Set<String> tableNames() throws SQLException {
        Set<String> tables = new HashSet<>();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'public'
                       and table_type = 'BASE TABLE'
                     """)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }
        }

        return tables;
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

    private static long rowCount(String tableName) throws SQLException {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); PreparedStatement statement = connection.prepareStatement("select count(*) from " + tableName);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
