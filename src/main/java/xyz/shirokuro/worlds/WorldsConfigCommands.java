package xyz.shirokuro.worlds;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.shirokuro.commandutility.CompletionData;
import xyz.shirokuro.commandutility.ExecutionData;
import xyz.shirokuro.commandutility.annotation.Completer;
import xyz.shirokuro.commandutility.annotation.Executor;
import xyz.shirokuro.commandutility.CompletionData;

public final class WorldsConfigCommands {
    private final WorldConfigList worldConfigList;
    private final Config config;

    public WorldsConfigCommands(final Config config, final WorldConfigList worldConfigList) {
        this.config = Objects.requireNonNull(config, "config");
        this.worldConfigList = Objects.requireNonNull(worldConfigList, "worldConfigList");
    }

    @Executor(command = "world config show [world]", description = "TODO")
    public void executeConfigShow(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = Optional.ofNullable(data.get("world"))
            .orElseGet(() -> {
                return sender instanceof Player
                    ? ((Player) sender).getWorld().getName()
                    : null;
            });
        if (worldName == null) {
            sender.sendMessage(ChatColor.RED + "Please specify world or perform from the game");
            return;
        }
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        sender.sendMessage(ChatColor.BOLD + "Settings of " + worldName);
        sender.sendMessage("  - keep-spawn-in-memory: " + worldConfig.keepSpawnInMemory());
        sender.sendMessage("  - game-mode: " + worldConfig.getGameMode().name());
        sender.sendMessage("  - time: " + worldConfig.getTime());
        sender.sendMessage("  - spawn:");
        final Coord spawn = worldConfig.getSpawn();
        if (spawn == null) {
            sender.sendMessage("    Not Specified");
        } else {
            sender.sendMessage("    - x: " + spawn.getX());
            sender.sendMessage("    - y: " + spawn.getY());
            sender.sendMessage("    - z: " + spawn.getZ());
            sender.sendMessage("    - yaw: " + spawn.getYaw());
            sender.sendMessage("    - pitch: " + spawn.getPitch());
        }
        sender.sendMessage("  - game-rules:");
        worldConfig.getGameRules().forEach((key, value) -> {
            sender.sendMessage("    - " + key + ": " + value);
        });
    }

    @Completer(command = "world config show [world]")
    public List<String> completeConfigShow(final CompletionData data) {
        return  worlds(data);
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

    @Executor(command = "world config spawn <world> <x> <y> <z> <yaw> <pitch>", description = "TODO")
    public void executeConfigSpawn(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        try {
            final double x = Double.parseDouble(data.get("x"));
            final double y = Double.parseDouble(data.get("y"));
            final double z = Double.parseDouble(data.get("z"));
            final float yaw = Float.parseFloat(data.get("yaw"));
            final float pitch = Float.parseFloat(data.get("pitch"));
            worldConfig.setSpawn(new Coord(x, y, z, yaw, pitch));
            saveWorldConfigList(sender);
            sender.sendMessage(ChatColor.GREEN + "Updated!");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }
    }

    @Completer(command = "world config spawn <world> <x> <y> <z> <yaw> <pitch>")
    public List<String> completeConfigSpawn(final CompletionData data) {
        if (data.getSender() instanceof Player) {
            final Player player = (Player) data.getSender();
            final Object result;
            switch (data.getName()) {
                case "world":
                    result = player.getWorld().getName();
                    break;
                case "x":
                    result = player.getLocation().getX();
                    break;
                case "y":
                    result = player.getLocation().getY();
                    break;
                case "z":
                    result = player.getLocation().getZ();
                    break;
                case "yaw":
                    result = player.getLocation().getYaw();
                    break;
                case "pitch":
                    result = player.getLocation().getPitch();
                    break;
                default:
                    throw new RuntimeException("unreachable");
            }
            return Collections.singletonList(result.toString());
        } else if (data.getName().equals("world")) {
            return worlds(data);
        } else {
            return Collections.emptyList();
        }
    }

    @Executor(command = "world config gamemode <world> <gamemode>", description = "TODO")
    public void executeConfigGameMode(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        final GameMode gameMode;
        try {
            gameMode = GameMode.valueOf(data.get("gamemode").toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + data.get("gamemode") + " is invalid game mode");
            return;
        }
        worldConfig.setGameMode(gameMode);
        saveWorldConfigList(sender);
        final World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getWorld().equals(world))
                .forEach(p -> {
                    worldConfig.updateGameModeIfNeeded(config, p);
                });
        }
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }

    @Completer(command = "world config gamemode <world> <gamemode>")
    public List<String> completeConfigGameMode(final CompletionData data) {
        switch (data.getName()) {
            case "world":
                return worlds(data);
            case "gamemode":
                return Arrays.stream(GameMode.values())
                    .map(GameMode::name)
                    .filter(s -> s.startsWith(data.getCurrentValue()))
                    .collect(Collectors.toList());
            default:
                throw new RuntimeException("unreachable");
        }
    }

    @Executor(command = "world config gamerule <world> <gamerule> <value>", description = "TODO")
    public void executeConfigGameRule(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        worldConfig.getGameRules().put(data.get("gamerule"), data.get("value"));
        saveWorldConfigList(sender);
        final World world = Bukkit.getWorld(worldName);
        if (world != null) {
            worldConfig.apply(world);
        }
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }

    private List<String> worlds(final CompletionData data) {
        return Bukkit.getWorlds().stream()
            .map(World::getName)
            .filter(w -> w.startsWith(data.getCurrentValue()))
            .collect(Collectors.toList());
    }

    @Completer(command = "world config gamerule <world> <gamerule> <value>")
    public List<String> completeConfigGameRule(final CompletionData data) {
        if (data.getName().equals("world")) {
            return worlds(data);
        } else {
            return Collections.emptyList();
        }
    }

    @Executor(command = "world config keepspawninmemory <world> <value>", description = "TODO")
    public void executeConfigKeepSpawnInMemory(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        worldConfig.setKeepSpawnInMemory(Boolean.parseBoolean(data.get("value")));
        saveWorldConfigList(sender);
        final World world = Bukkit.getWorld(worldName);
        if (world != null) {
            worldConfig.apply(world);
        }
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }

    @Completer(command = "world config keepspawninmemory <world> <value>")
    public List<String> completeConfigKeepSpawnInMemory(final CompletionData data) {
        if (data.getName().equals("world")) {
            return worlds(data);
        } else {
            return Collections.emptyList();
        }
    }

    @Executor(command = "world config time <world> <time>", description = "TODO")
    public void executeConfigTime(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        try {
            worldConfig.setTime(Integer.parseInt(data.get("time")));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }
        saveWorldConfigList(sender);
        final World world = Bukkit.getWorld(worldName);
        if (world != null) {
            worldConfig.apply(world);
        }
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }

    @Completer(command = "world config time <world> <time>")
    public List<String> completeConfigTime(final CompletionData data) {
        if (data.getName().equals("world")) {
            return worlds(data);
        } else {
            return Collections.emptyList();
        }
    }
}


