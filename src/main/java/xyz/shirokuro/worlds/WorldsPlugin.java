package xyz.shirokuro.worlds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import xyz.shirokuro.commandutility.CommandGroup;
import xyz.shirokuro.commandutility.CommandGroup;

public final class WorldsPlugin implements Listener {

    private static WorldsPlugin instance;

    private final WorldConfigList worldConfigList;
    private final Config config;

    public WorldsPlugin(final Plugin plugin) throws PluginInitException {
        plugin.saveDefaultConfig();
        final Path dataFolder = plugin.getDataFolder().toPath();
        try {
            this.worldConfigList = new WorldConfigList(dataFolder.resolve("worlds.yml"));
        } catch (IOException | ConfigException e) {
            throw new PluginInitException("Failed to load world list", e);
        }
        try {
            this.config = new Config(dataFolder.resolve("config.yml"));
        } catch (IOException | ConfigException e) {
            throw new PluginInitException("Failed to load configuration", e);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        new CommandGroup(ChatColor.RED.toString())
            .generateHelp(ChatColor.BOLD + "Worlds: Help")
            .addAll(new WorldsCommands(plugin, config, worldConfigList))
            .addAll(new WorldsConfigCommands(config, worldConfigList));
        final Path worldContainer = Bukkit.getWorldContainer().toPath();
        worldConfigList.getMap().forEach((worldName, worldConfig) -> {
            if (Files.notExists(worldContainer.resolve(worldName))) {
                plugin.getLogger().warning("World: " + worldName + " is registered in worlds.yml but not exist!");
                return;
            }
            final World world = WorldCreator.name(worldName).createWorld();
            worldConfig.apply(world);
        });
    }

    public static WorldsPlugin getInstance() {
        return instance;
    }

    public static void init(@NonNull final Plugin plugin) throws PluginInitException {
        if (instance != null) {
            throw new PluginInitException("Plugin already initialized!");
        }
        instance = new WorldsPlugin(plugin);
    }

    public WorldConfigList getWorldConfigList() {
        return worldConfigList;
    }

    public Config getWrappedConfig() {
        return config;
    }

    @EventHandler
    public void onWorldChange(final PlayerTeleportEvent event) {
        final World from = event.getFrom().getWorld();
        final World to = event.getTo().getWorld();
        if (from.equals(to)) {
            return;
        }
        worldConfigList.get(to).ifPresent(worldConfig -> {
            final Player player = event.getPlayer();
            worldConfig.updateGameModeIfNeeded(config, player);
        });
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        worldConfigList.get(player.getWorld()).ifPresent(worldConfig -> {
            worldConfig.updateGameModeIfNeeded(config, player);
        });
    }
}
