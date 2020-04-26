package xyz.shirokuro.worlds;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.concurrent.ConcurrentMap;

public final class WorldConfigList {

    private final Path filePath;
    private ConcurrentMap<String, WorldConfig> map = new ConcurrentHashMap<>();

    public WorldConfigList(final Path filePath) throws IOException, ConfigException {
        this.filePath = Objects.requireNonNull(filePath);
        reload();
    }

    public void reload() throws IOException, ConfigException {
        if (Files.notExists(filePath)) {
            return;
        }
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Path: " + filePath + " is not a regular file!");
        }
        final ConcurrentMap<String, WorldConfig> map = new ConcurrentHashMap<>();
        final YamlConfiguration rootConfig = YamlConfiguration.loadConfiguration(filePath.toFile());
        for (final String worldName : rootConfig.getKeys(false)) {
            final ConfigurationSection  worldConfSection = rootConfig.getConfigurationSection(worldName);
            final WorldConfig worldConfig = WorldConfig.load(worldConfSection);
            map.put(worldName, worldConfig);
        }
        this.map = map;
    }

    public CompletableFuture<Void> save() {
        return CompletableFuture.runAsync(() -> {
            final YamlConfiguration config = new YamlConfiguration();
            map.forEach((worldName, worldConfig) -> {
                final ConfigurationSection worldConfigSection = config.createSection(worldName);
                worldConfig.fillConfigurationSection(worldConfigSection);
            });
            try {
                config.save(filePath.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Returns {@code WorldConfig} by specified world name.
     * If specified world is not managed by Worlds, it will returns empty {@code Optional}.
     *
     * @param worldName name of the world
     * @return maybe empty
     */
    public Optional<WorldConfig> get(final String worldName) {
        return Optional.ofNullable(map.get(worldName));
    }

    /**
     * Returns {@code WorldConfig} by specified world.
     * If specified world is not managed by Worlds, it will returns empty {@code Optional}.
     *
     * @param world instance of {@code World}
     * @return maybe empty
     */
    public Optional<WorldConfig> get(final World world) {
        return get(world.getName());
    }

    public void add(final String worldName, final WorldConfig worldConfig) {
        map.put(worldName, worldConfig);
    }

    public void add(final World world, final WorldConfig worldConfig) {
        add(world.getName(), worldConfig);
    }

    public void remove(final String worldName) {
        map.remove(worldName);
    }

    public ConcurrentMap<String, WorldConfig> getMap() {
        return map;
    }
}
