package com.github.curiousoddman.curioustestutils.testcontainer.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
public class PostgresContainerConfig {

    private static final String USERNAME = "user1";
    private static final String PASSWORD = "pass1";
    private static final String DB_NAME = "dbName";
    @Value("${curious.postgres-test-container.image}")
    private String postgresTestContainerImage;

    @Bean
    public PostgreSQLContainer<?> container() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(postgresTestContainerImage).asCompatibleSubstituteFor("postgres"));
        container.withUsername(USERNAME);
        container.withPassword(PASSWORD);
        container.withDatabaseName(DB_NAME);
        container.waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1));
        container.start();
        log.info("PostgreSQL container started. jdbcUrl:[{}], username:[{}], password:[{}]",
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword()
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping PostgreSQL test container...");
            container.stop();
        }));
        return container;
    }

    @Bean
    @ConditionalOnProperty(name = "curious.test.container.current.schema.name")
    public DataSource dataSource(PostgreSQLContainer<?> postgreSQLContainer, @Value("${curious.test.container.current.schema.name}") String schemaName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(postgreSQLContainer.getJdbcUrl() + "&" + schemaParam(schemaName));
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setSchema(schemaName);
        log.info("{} dataSource: {}", schemaName, dataSource);
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(name = "curious.test.container.current.schema.name")
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("jdbcTemplate: {}", jdbcTemplate);
        return jdbcTemplate;
    }

    @Bean
    public TestCleanUpService testCleanUpService(List<JdbcTemplate> jdbcTemplates) {
        return new TestCleanUpService(jdbcTemplates);
    }

    private String schemaParam(String schemaName) {
        return "currentSchema=" + schemaName;
    }
}
