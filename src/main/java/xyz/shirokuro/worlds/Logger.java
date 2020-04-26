package xyz.shirokuro.worlds;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class Logger {
    private static volatile java.util.logging.Logger wrapped = null;

    public static void init(java.util.logging.Logger wrapped) {
        if (Logger.wrapped != null) {
            throw new IllegalStateException("Logger already initialized");
        }
        Logger.wrapped = wrapped;
    }

    public static void debug(final Supplier<String> message) {
        checkState();
        Objects.requireNonNull(message, "message");
        wrapped.fine(message);
    }

    public static void info(final String message) {
        checkState();
        Objects.requireNonNull(message, "message");
        wrapped.info(message);
    }

    public static void warn(final String message, final Throwable thrown) {
        checkState();
        Objects.requireNonNull(message, "message");
        wrapped.log(Level.WARNING, message, thrown);
    }

    public static void warn(final String message) {
        warn(message, null);
    }

    public static void error(final String message, final Throwable thrown) {
        checkState();
        Objects.requireNonNull(message);
        wrapped.log(Level.SEVERE, message, thrown);
    }

    public static void error(final String message) {
        error(message, null);
    }

    private static void checkState() {
        if (wrapped == null) {
            throw new IllegalStateException("Logger is not initialized yet");
        }
    }
}


