package com.viruchith.PromptButler.core.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class AppLoggerTest {

    @Test
    void singleton() {
        assertSame(AppLogger.get(), AppLogger.get());
    }

    @Test
    void loggingBranches() {
        AppLogger log = AppLogger.get();
        log.setVerbose(true);
        log.info("info-line");
        log.warn("warn-line");
        log.error("err-line", new RuntimeException("boom"));
        log.setVerbose(false);
        log.info("suppressed");
    }
}
