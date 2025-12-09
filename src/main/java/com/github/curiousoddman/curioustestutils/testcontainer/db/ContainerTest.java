package com.github.curiousoddman.curioustestutils.testcontainer.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = PostgresContainerConfig.class)
public abstract class ContainerTest {
    @Autowired
    private TestCleanUpService cleanUpService;

    @AfterEach
    public void cleanUp() {
        log.info("Performing database cleanup");
        if (shouldCleanup()) {
            cleanUpService.cleanUp();
        } else {
            log.info("Cleanup skipped!");
        }
    }

    public boolean shouldCleanup() {
        return true;
    }
}
