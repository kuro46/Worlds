package xyz.shirokuro.worlds;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class Config {

    private final Path filePath;
    private boolean updateGameModeForAdmin;
    private WorldConfig defaultWorldConfig;
    private WorldCreationConfig defaultWorldCreationConfig;

    public Config(final Path filePath) throws ConfigException, IOException {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        reload();
    }

    public void reload() throws ConfigException, IOException {
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Path: " + filePath + " is not a regular file!");
        }
        if (Files.notExists(filePath)) {
            throw new IOException("File: " + filePath + " does not exist!");
        }
        final YamlConfiguration conf = YamlConfiguration.loadConfiguration(filePath.toFile());
        if (!conf.contains("update-game-mode-for-admin")) {
            throw new ConfigKeyNotPresentException("update-game-mode-for-admin");
        }
        this.updateGameModeForAdmin = conf.getBoolean("update-game-mode-for-admin");
        if (!conf.contains("default-world-config")) {
            throw new ConfigKeyNotPresentException("default-world-config");
        }
        this.defaultWorldConfig = WorldConfig.load(conf.getConfigurationSection("default-world-config"));
        if (!conf.contains("default-creation-config")) {
            throw new ConfigKeyNotPresentException("default-creation-config");
        }
        this.defaultWorldCreationConfig = WorldCreationConfig.load(conf.getConfigurationSection("default-creation-config"));
    }

    public CompletableFuture save() {
        return CompletableFuture.runAsync(() -> {
            final YamlConfiguration rootConfig = new YamlConfiguration();
            rootConfig.set("update-game-mode-for-admin", updateGameModeForAdmin);
            final ConfigurationSection defaultWorldConfigSection = rootConfig.createSection("default-world-config");
            defaultWorldConfig.fillConfigurationSection(defaultWorldConfigSection);
            final ConfigurationSection defaultWorldCreationConfigSection = rootConfig.createSection("default-creation-config");
            defaultWorldCreationConfig.fillConfigurationSection(defaultWorldCreationConfigSection);
            try {
                rootConfig.save(filePath.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void setUpdateGameModeForAdmin(final boolean updateGameModeForAdmin) {
        this.updateGameModeForAdmin = updateGameModeForAdmin;
    }

    public boolean doUpdateGameModeForAdmin() {
        return updateGameModeForAdmin;
    }

    public WorldConfig getDefaultWorldConfig() {
        return defaultWorldConfig;
    }

    public WorldCreationConfig getDefaultWorldCreationConfig() {
        return defaultWorldCreationConfig;
    }
}
