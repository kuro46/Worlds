package xyz.shirokuro.worlds;

public final class ConfigKeyNotPresentException extends ConfigException {

    private final String key;

    public ConfigKeyNotPresentException(final String key) {
        super("Config key: '" + key + "' not present");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}


