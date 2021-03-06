package xyz.shirokuro.worlds;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class WorldConfig {

    private boolean keepSpawnInMemory;
    private GameMode gameMode;
    private Coord spawnCoord;

    private WorldConfig(
        final boolean keepSpawnInMemory,
        @NonNull final GameMode gameMode,
        final Coord spawnCoord) {
        this.keepSpawnInMemory = keepSpawnInMemory;
        this.spawnCoord = spawnCoord;
        this.gameMode = gameMode;
    }

    public static WorldConfig copy(final WorldConfig source) {
        return new WorldConfig(source.keepSpawnInMemory, source.gameMode, source.spawnCoord);
    }

    public static WorldConfig fromDefault(final DefaultWorldConfig def) {
        return new WorldConfig(def.keepSpawnInMemory(), def.getGameMode(), null);
    }

    public static WorldConfig load(@NonNull final ConfigurationSection section)
        throws ConfigException {

        final Coord spawnCoord = section.contains("spawn")
            ? Coord.fromConfigSection(section.getConfigurationSection("spawn"))
            : null;
        if (!section.contains("game-mode")) {
            throw new ConfigKeyNotPresentException("game-mode");
        }
        final String gameModeStr = section.getString("game-mode");
        final GameMode gameMode;
        try {
            gameMode = GameMode.valueOf(gameModeStr.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException ignored) {
            throw new ConfigException(gameModeStr + " is invalid GameMode");
        }
        if (!section.contains("keep-spawn-in-memory")) {
            throw new ConfigKeyNotPresentException("keep-spawn-in-memory");
        }
        final boolean keepSpawnInMemory = section.getBoolean("keep-spawn-in-memory");
        return new WorldConfig(keepSpawnInMemory, gameMode, spawnCoord);
    }

    public void fillConfigurationSection(final ConfigurationSection section) {
        section.set("game-mode", gameMode.name());
        section.set("keep-spawn-in-memory", keepSpawnInMemory);
        if (spawnCoord != null) {
            spawnCoord.fillConfigSection(section.createSection("spawn"));
        }
    }

    public boolean keepSpawnInMemory() {
        return keepSpawnInMemory;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    /**
     * Returns spawn coordinate of this config.
     * If spawn is not specified, it will returns empty Optional.
     *
     * @return {@code Coord} or empty
     */
    public Optional<Coord> getSpawn() {
        return Optional.ofNullable(spawnCoord);
    }

    public void setSpawn(final Coord spawn) {
        this.spawnCoord = spawn;
    }

    public void setKeepSpawnInMemory(boolean keepSpawnInMemory) {
        this.keepSpawnInMemory = keepSpawnInMemory;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void updateGameModeIfNeeded(final Config config, final Player player) {
        if (config.doUpdateGameModeForAdmin() || !player.hasPermission("worlds.admin")) {
            player.setGameMode(gameMode);
        }
    }

    /**
     * Apply this configuration to specified world.
     */
    public void apply(final World world) {
        Objects.requireNonNull(world, "world");
        world.setKeepSpawnInMemory(keepSpawnInMemory);
    }
}
