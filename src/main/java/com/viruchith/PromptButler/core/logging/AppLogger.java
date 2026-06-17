package com.viruchith.PromptButler.core.logging;

import com.viruchith.PromptButler.core.model.BuildProfile;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Minimal logging facade (stderr). Verbose in {@link BuildProfile#DEV} only.
 */
public final class AppLogger {

    private static final AppLogger INSTANCE = new AppLogger();

    private final PrintStream err = System.err;
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
            err.println("[prompt-butler] INFO: " + message);
        }
    }

    public void warn(String message) {
        err.println("[prompt-butler] WARN: " + Objects.requireNonNull(message, "message"));
    }

    public void warn(String message, Throwable t) {
        err.println("[prompt-butler] WARN: " + Objects.requireNonNull(message, "message"));
        if (t != null) {
            t.printStackTrace(err);
        }
    }

    public void error(String message, Throwable t) {
        err.println("[prompt-butler] ERROR: " + Objects.requireNonNull(message, "message"));
        if (t != null) {
            t.printStackTrace(err);
        }
    }
}
