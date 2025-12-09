package com.github.curiousoddman.curioustestutils.testcontainer.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "curious.test.container.current.schema.name = schema_name")
class TestCleanUpServiceTest extends ContainerTest {
    public static final String INSERT_INTO_EXISTING_TABLE = """
            	INSERT INTO test
            	(a, b, c)
            	VALUES ('a', 'b', 'c');
            """;

    @Autowired
    List<JdbcTemplate> jdbcTemplates;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TestCleanUpService testCleanUpService;

    @BeforeAll
    void beforeAllLocal() {
        // Trigger full cleanup
        testCleanUpService.cleanUp();
        log.info("beforeAllLocal done!");
    }

    @Test
    void basicCleanuptTest() {
        assertDoesNotThrow(() -> testCleanUpService.cleanUp());
    }

    @Test
    @Sql(statements = INSERT_INTO_EXISTING_TABLE)
    void verifyThatSqlAnnotatedDataIsGettingCleanedUp() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_risk_params_lgd", Integer.class);
        assertEquals(1, count);
        testCleanUpService.cleanUp();
        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_risk_params_lgd", Integer.class);
        assertEquals(0, count);
    }

    @Test
    @Sql(statements = {"CREATE TABLE test_cleanup_table (a VARCHAR)", "INSERT INTO test_cleanup_table VALUES ('xx'), ('uu')"})
    void verifySqlAnnotatedCreatedTableGetsCleanedUp() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_cleanup_table", Integer.class);
        assertEquals(2, count);
        testCleanUpService.cleanUp();

        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_cleanup_table", Integer.class);
        assertEquals(0, count);
    }

    @Test
    void verifyTableInCodeGetsCleanedUp() {
        jdbcTemplate.update(INSERT_INTO_EXISTING_TABLE);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_risk_params_lgd", Integer.class);
        assertEquals(1, count);
        testCleanUpService.cleanUp();
        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_risk_params_lgd", Integer.class);
        assertEquals(0, count);
    }

    @Test
    void verifyTableCreatedInCodeGetsCleanedUp() {
        jdbcTemplate.update("CREATE TABLE test_code_cleanup_table (a VARCHAR)");
        jdbcTemplate.update("INSERT INTO test_code_cleanup_table VALUES ('xx'), ('uu')");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_code_cleanup_table", Integer.class);
        assertEquals(2, count);
        testCleanUpService.cleanUp();

        count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_code_cleanup_table", Integer.class);
        assertEquals(0, count);
    }
}
