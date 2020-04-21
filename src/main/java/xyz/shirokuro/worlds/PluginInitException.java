package xyz.shirokuro.worlds;

import lombok.NonNull;

public final class PluginInitException extends Exception {
    public PluginInitException(@NonNull final String message) {
        super(message);
    }

    public PluginInitException(@NonNull final String message, @NonNull final Throwable cause) {
        super(message, cause);
    }
}
