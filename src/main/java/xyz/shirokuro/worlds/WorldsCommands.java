package xyz.shirokuro.worlds;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.shirokuro.commandutility.CompletionData;
import xyz.shirokuro.commandutility.ExecutionData;
import xyz.shirokuro.commandutility.annotation.Completer;
import xyz.shirokuro.commandutility.annotation.Executor;
import xyz.shirokuro.commandutility.CompletionData;

public final class WorldsCommands {

    private final WorldConfigList worldConfigList;
    private final Config config;
    private final Plugin plugin;

    public WorldsCommands(final Plugin plugin ,final Config config, final WorldConfigList worldConfigList) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.worldConfigList = Objects.requireNonNull(worldConfigList, "worldConfigList");
    }

    @Executor(command = "world list", description = "TODO")
    public void executeList(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        sender.sendMessage(ChatColor.BOLD + "Worlds:");
        Bukkit.getWorlds().stream()
            .map(World::getName)
            .sorted(Comparator.naturalOrder())
            .forEach(worldName -> {
                final String status = worldConfigList.get(worldName).isPresent()
                    ? ChatColor.GREEN + "Managed"
                    : ChatColor.RED + "Not managed";
                sender.sendMessage("  - " + worldName + ChatColor.GRAY +
                        " (" + status + ChatColor.GRAY + ")");
            });
        sender.sendMessage(ChatColor.BOLD + "Can't be loaded:");
        worldConfigList.getMap().keySet().stream()
            .filter(w -> Bukkit.getWorld(w) == null)
            .sorted(Comparator.naturalOrder())
            .forEach(w -> sender.sendMessage("  - " + w));
    }

    private void saveWorldConfigList(final CommandSender sender) {
        worldConfigList.save()
            .exceptionally(t -> {
                sender.sendMessage(ChatColor.RED + "Failed to save configuration! Error: " +
                        t.getMessage());
                System.err.println("An exception occurred while saving world configuration");
                t.printStackTrace();
                return null;
            });
    }

    @Executor(command = "world import <worldName>", description = "")
    public void executeImport(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("worldName");
        if (worldConfigList.get(worldName).isPresent()) {
            sender.sendMessage(ChatColor.RED + "World: " +
                    ChatColor.GRAY + worldName + ChatColor.RED +
                    " is already imported");
            return;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World: " +
                    ChatColor.GRAY + worldName + ChatColor.RED +
                    " not found");
        }
        final WorldConfig worldConfig = WorldConfig.copy(config.getDefaultWorldConfig());
        worldConfig.apply(world);
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().equals(world))
            .forEach(p -> worldConfig.updateGameModeIfNeeded(config, p));
        worldConfigList.add(worldName, worldConfig);
        sender.sendMessage(ChatColor.GREEN + "Imported!");
        saveWorldConfigList(sender);
    }

    @Completer(command = "world import <worldName>")
    public List<String> completeImport(final CompletionData data) {
        if (data.getName().equals("worldName")) {
            return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(w -> !worldConfigList.get(w).isPresent())
                .filter(w -> w.startsWith(data.getCurrentValue()))
                .collect(Collectors.toList());
        } else {
            throw new RuntimeException("Unreachable");
        }
    }

    @Executor(command = "world reload", description = "TODO")
    public void executeReload(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        CompletableFuture.runAsync(() -> {
            sender.sendMessage(ChatColor.GRAY + "Reloading...");
            try {
                config.reload();
                worldConfigList.reload();
            } catch (IOException | ConfigException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getWorlds().forEach(w -> {
                    worldConfigList.get(w).ifPresent(worldConfig -> {
                        worldConfig.apply(w);
                    });
                });
                Bukkit.getOnlinePlayers().forEach(p -> {
                    worldConfigList.get(p.getWorld()).ifPresent(worldConfig -> {
                        worldConfig.updateGameModeIfNeeded(config, p);
                    });
                });
                sender.sendMessage(ChatColor.GREEN + "Reloaded!");
            });
        }).exceptionally(t -> {
            sender.sendMessage(ChatColor.RED +
                    "Failed to reload configuration! Error: " + t.getMessage());
            System.err.println("An exception occurred while reloading configuration");
            t.printStackTrace();
            return null;
        });
    }

    @Executor(command = "world remove <worldName>", description = "TODO")
    public void executeRemove(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("worldName");
        if (!worldConfigList.get(worldName).isPresent()) {
            sender.sendMessage(ChatColor.RED + "World: " +
                    ChatColor.GRAY + worldName + ChatColor.RED +
                    " not found");
            return;
        }
        worldConfigList.remove(worldName);
        sender.sendMessage(ChatColor.GREEN + "Removed!");
        saveWorldConfigList(sender);
    }

    @Completer(command = "world remove <worldName>")
    public List<String> completeRemove(final CompletionData data) {
        if (data.getName().equals("worldName")) {
            return worldConfigList.getMap().keySet().stream()
                .filter(w -> w.startsWith(data.getCurrentValue()))
                .collect(Collectors.toList());
        } else {
            throw new RuntimeException("Unreachable");
        }
    }

    @Executor(command = "world create <worldName>", description = "TODO")
    public void executeCreate(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("worldName");
        if (worldConfigList.get(worldName).isPresent()) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " is already exist in worlds.yml");
            return;
        }
        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " is already exist");
            return;
        }
        sender.sendMessage(ChatColor.GRAY + "Creating...");
        final WorldCreator creator = WorldCreator.name(worldName);
        config.getDefaultWorldCreationConfig().configureWorldCreator(creator);
        final World world = creator.createWorld();
        final WorldConfig worldConfig = WorldConfig.copy(config.getDefaultWorldConfig());
        worldConfigList.add(world, worldConfig);
        saveWorldConfigList(sender);
        sender.sendMessage(ChatColor.GREEN + "Created!");
    }

    @Executor(command = "world tp <world>", description = "TODO")
    public void executeTP(final ExecutionData data) {
        final Player player = data.getSenderAsPlayer();
        if (player == null) {
            data.getSender().sendMessage(ChatColor.RED + "Cannot perform this command from the console");
            return;
        }
        final World world = Bukkit.getWorld(data.get("world"));
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World: " + data.get("world") + " not found");
            return;
        }
        final Location dest = worldConfigList.get(world)
            .map(worldConfig -> worldConfig.getSpawn())
            .map(coord -> coord.withWorld(world))
            .orElse(world.getSpawnLocation());
        player.teleport(dest);
    }

    @Completer(command = "world tp <world>")
    public List<String> completeTP(final CompletionData data) {
        if (data.getName().equals("world")) {
            return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(w -> w.startsWith(data.getCurrentValue()))
                .collect(Collectors.toList());
        } else {
            throw new RuntimeException("unreachable");
        }
    }
}
