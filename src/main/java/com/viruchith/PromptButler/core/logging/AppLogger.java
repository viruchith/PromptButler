package com.viruchith.PromptButler.core.logging;

import com.viruchith.PromptButler.core.model.BuildProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Minimal logging facade. Verbose info and stack traces stay gated in {@link BuildProfile#DEV} only.
 */
public final class AppLogger {

    private static final AppLogger INSTANCE = new AppLogger();
    private static final Logger LOG = LoggerFactory.getLogger("prompt-butler");

    private volatile boolean verbose = BuildProfile.current().isDev();

    private AppLogger() {
    }

    public static AppLogger get() {
        return INSTANCE;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void info(String message) {
        Objects.requireNonNull(message, "message");
        if (verbose) {
            LOG.info(message);
        }
    }

    public void warn(String message) {
        LOG.warn(Objects.requireNonNull(message, "message"));
    }

    public void warn(String message, Throwable t) {
        logThrowableAtWarn(Objects.requireNonNull(message, "message"), t);
    }

    public void error(String message, Throwable t) {
        String safeMessage = Objects.requireNonNull(message, "message");
        if (t != null) {
            if (verbose) {
                LOG.error(safeMessage, t);
            } else {
                LOG.error(safeMessage + " | Caused by: " + summarizeThrowable(t));
            }
            return;
        }
        LOG.error(safeMessage);
    }

    private void logThrowableAtWarn(String message, Throwable t) {
        if (t == null) {
            LOG.warn(message);
            return;
        }
        if (verbose) {
            LOG.warn(message, t);
            return;
        }
        LOG.warn(message + " | Caused by: " + summarizeThrowable(t));
    }

    private static String summarizeThrowable(Throwable t) {
        String text = t.getClass().getName();
        String detail = t.getMessage();
        if (detail == null || detail.isEmpty()) {
            return text;
        }
        return text + ": " + detail;
    }
}
