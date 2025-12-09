package com.github.curiousoddman.curioustestutils.testcontainer.db;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Slf4j
@RequiredArgsConstructor
public class TestCleanUpService {
	private final List<JdbcTemplate> jdbcTemplateList;

	private static final String CHANGES_TABLE_NAME = "t_tests_changes_tracking";
	public final Set<JdbcTemplate> initializedTemplates = new HashSet<>();

	public void cleanUp() {
		for (JdbcTemplate jdbcTemplate : jdbcTemplateList) {
			Instant start = Instant.now();
			try (Connection conn = getConnection(jdbcTemplate)) {
				String schema = conn.getSchema();
				MDC.put("context", schema);
				log.info("Initialize...");
				Set<String> tables = new HashSet<>();

				if (shouldInitialize(jdbcTemplate)) {
					initialize(schema, jdbcTemplate);

					log.info("Cleaning up all existing tables in schema '{}'", schema);
					requireNonNull(schema, "When schema is null tables from all schemas will be cleared. Please set schema using DataSource::setSchema()");
					tables.addAll(getAllTablesInSchema(conn, schema));
				} else {
					tables.addAll(getAndClearModifiedTables(jdbcTemplate));
					log.info("Cleaning tables incrementally in schema {} (only the ones that were changed during test).", schema);
				}

				truncateTables(conn, schema, tables);
			} catch (SQLException e) {
				log.error("Error fetching table metadata in postgres tests. Make sure test isolation is still fine.", e);
				throw new IllegalStateException(e);
			} finally {
				log.info("Cleanup completed in {}", Duration.between(start, Instant.now()));
				MDC.clear();
			}
		}
	}

	private Set<String> getAllTablesInSchema(Connection conn, String schema) throws SQLException {
		Set<String> tables = new HashSet<>();
		ResultSet rs = conn.getMetaData().getTables(null, schema, null, new String[]{"TABLE"});
		while (rs.next()) {
			tables.add(rs.getString(3));
		}
		return tables;
	}

	private boolean shouldInitialize(JdbcTemplate jdbcTemplate) {
		if (initializedTemplates.contains(jdbcTemplate)) {
			return false;
		} else {
			return jdbcTemplate.queryForObject("SELECT to_regclass('" + CHANGES_TABLE_NAME + "');", Object.class) == null;
		}
	}

	@SneakyThrows
	private void initialize(String schema, JdbcTemplate jdbcTemplate) {
		String currentSchema = jdbcTemplate.queryForObject("SHOW search_path;", String.class);
		log.info("Initializing schema {}", currentSchema);
		executeFile("on_row_inserted_function.sql", jdbcTemplate);
		executeFile("track_inserts_function.sql", jdbcTemplate);
		executeFile("create_table_event_trigger_if_not_exists.sql", jdbcTemplate);
		executeFile("create_table.sql", jdbcTemplate);
		try (Connection connection = getConnection(jdbcTemplate)) {
			Set<String> allTablesInSchema = getAllTablesInSchema(connection, schema);
			for (String tableName : allTablesInSchema) {
				jdbcTemplate.execute("SELECT test_track_inserts('" + tableName + "'::regclass)");        // NOSONAR
			}
		}

		initializedTemplates.add(jdbcTemplate);
		log.info("Initialized schema {} ", schema);
	}

	@SneakyThrows
	private void executeFile(String fileName, JdbcTemplate jdbcTemplate) {
		ClassPathResource classPathResource = new ClassPathResource("cleanup/" + fileName);
		InputStream resource = classPathResource.getInputStream();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
			String contents = reader.lines().collect(Collectors.joining("\n"));
			jdbcTemplate.update(contents);
		}
	}

	public List<String> getAndClearModifiedTables(JdbcTemplate jdbcTemplate) throws SQLException {
		List<String> tableNames = jdbcTemplate.queryForList("SELECT DISTINCT table_name FROM " + CHANGES_TABLE_NAME, String.class);
		jdbcTemplate.update("TRUNCATE TABLE " + CHANGES_TABLE_NAME);
		return tableNames;
	}

	@SneakyThrows
	private void truncateTables(Connection connection, String schema, Set<String> tables) {
		if (tables.isEmpty()) {
			log.warn("Empty list of tables. Skipping cleanup for schema {} ", schema);
			return;
		}
		String formattedTableNames = String.join(",", tables);
		log.info("Truncating tables:[{}]", formattedTableNames);
		try (Statement statement = connection.createStatement()) {
			// Disable all triggers and constraints
			statement.execute("set session_replication_role to replica");
			final String truncateStmt = format("truncate %s CASCADE", formattedTableNames);

			try (PreparedStatement stmt = connection.prepareStatement(truncateStmt)) {
				stmt.execute();
			}

			// Re-enable all triggers and constraints
			statement.execute("set session_replication_role to default");
		}
	}

	private Connection getConnection(JdbcTemplate jdbcTemplate) throws SQLException {
		return requireNonNull(
				requireNonNull(
						requireNonNull(jdbcTemplate, "JdbcTemplate is null").
								getDataSource(), "DataSource is null")
						.getConnection(), "Connections is null"
		);
	}
}
