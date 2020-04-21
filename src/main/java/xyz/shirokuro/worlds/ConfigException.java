package xyz.shirokuro.worlds;

public class ConfigException extends Exception {
    public ConfigException(final String message) {
        super(message);
    }

    public ConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }
}


