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

    private final ConcurrentMap<String, String> gameRules = new ConcurrentHashMap<>();
    private boolean keepSpawnInMemory;
    private GameMode gameMode;
    private Coord spawnCoord;
    private int time;

    private WorldConfig(
        final boolean keepSpawnInMemory,
        @NonNull final GameMode gameMode,
        final Coord spawnCoord,
        final int time,
        @NonNull final Map<String, String> gameRules) {
        this.keepSpawnInMemory = keepSpawnInMemory;
        this.spawnCoord = spawnCoord;
        this.gameMode = gameMode;
        this.time = time;
        this.gameRules.putAll(gameRules);
    }

    public static WorldConfig copy(final WorldConfig source) {
        return new WorldConfig(source.keepSpawnInMemory, source.gameMode, source.spawnCoord, source.time, source.gameRules);
    }

    public static WorldConfig load(@NonNull final ConfigurationSection section)
        throws ConfigException {

        if (!section.contains("time")) {
            throw new ConfigKeyNotPresentException("time");
        }
        final int time = section.getInt("time");
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
        final Map<String, String> gameRules = new HashMap<>();
        final ConfigurationSection gameRulesSection = section.getConfigurationSection("game-rules");
        if (gameRulesSection != null) {
            for (String key : gameRulesSection.getKeys(false)) {
                gameRules.put(key, gameRulesSection.getString(key));
            }
        }
        return new WorldConfig(keepSpawnInMemory, gameMode, spawnCoord, time, gameRules);
    }

    public void fillConfigurationSection(final ConfigurationSection section) {
        section.set("game-mode", gameMode.name());
        section.set("keep-spawn-in-memory", keepSpawnInMemory);
        section.set("time", time);
        final ConfigurationSection gameRulesSection = section.createSection("game-rules");
        gameRules.forEach((key, value) -> {
            gameRulesSection.set(key, value);
        });
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
     * Returns gamerules.
     *
     * @return Map (Mutable)
     */
    public Map<String, String> getGameRules() {
        return gameRules;
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

    public int getTime() {
        return time;
    }

    public void setTime(final int time) {
        this.time = time;
    }

    /**
     * Apply this configuration to specified world.
     */
    public void apply(final World world) {
        Objects.requireNonNull(world, "world");
        world.setKeepSpawnInMemory(keepSpawnInMemory);
        if (time != -1) {
            world.setTime(time);
        }
        gameRules.forEach((key, value) -> {
            if (!world.isGameRule(key)) {
                throw new IllegalArgumentException("Gamerule: " + key + " is invalid gamerule!");
            }
            world.setGameRuleValue(key, value);
        });
    }
}
