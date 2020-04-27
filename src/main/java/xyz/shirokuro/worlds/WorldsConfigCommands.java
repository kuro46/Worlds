package xyz.shirokuro.worlds;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    @Executor(
        command = "world config show [world:worlds]",
        description = "Show current/specified world's configuration"
    )
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
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Please make sure '" + worldName + "' is loaded!");
            return;
        }
        sender.sendMessage(ChatColor.BOLD + "Settings of " + worldName);
        sender.sendMessage("  - keep-spawn-in-memory: " + world.getKeepSpawnInMemory());
        sender.sendMessage("  - time: " + world.getTime());
        sender.sendMessage("  - gamerules:");
        for (final String key : world.getGameRules()) {
            sender.sendMessage("    - " + key + ": " + world.getGameRuleValue(key));
        }
        final WorldConfig worldConfig = worldConfigList.get(worldName).orElse(null);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + "gamemode and spawn are skipped due to this" +
                    " world is not managed by this plugin.");
            return;
        }
        sender.sendMessage("  - gamemode: " + worldConfig.getGameMode().name());
        sender.sendMessage("  - spawn:");
        final Location worldSpawn = world.getSpawnLocation();
        final Coord spawn = worldConfig.getSpawn().orElse(null);
        sender.sendMessage("    - x: " + (spawn == null ? worldSpawn.getX() : spawn.getX()));
        sender.sendMessage("    - y: " + (spawn == null ? worldSpawn.getY() : spawn.getY()));
        sender.sendMessage("    - z: " + (spawn == null ? worldSpawn.getZ() : spawn.getZ()));
        sender.sendMessage("    - yaw: " + (spawn == null ? worldSpawn.getYaw() : spawn.getYaw()));
        sender.sendMessage("    - pitch: " + (spawn == null ? worldSpawn.getPitch() : spawn.getPitch()));
    }

    private void saveWorldConfigList(final CommandSender sender) {
        worldConfigList.save()
            .exceptionally(t -> {
                sender.sendMessage(ChatColor.RED + "Failed to save configuration! Error: " +
                        t.getMessage());
                Logger.error("An exception occurred while saving world configuration", t);
                return null;
            });
    }

    @Executor(
        command = "world config spawn <world> <x> <y> <z> <yaw> <pitch>",
        description = "Set spawn location of specified world"
    )
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
                    result = player.getLocation().getWorld().getName();
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
            final String resultStr = result.toString();
            return resultStr.startsWith(data.getCurrentValue())
                ? Collections.singletonList(resultStr)
                : Collections.emptyList();
        } else if (data.getName().equals("world")) {
            return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(w -> w.startsWith(data.getCurrentValue()))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Executor(
        command = "world config gamemode <world:managedworlds> <gamemode>",
        description = "Set gamemode of specified world"
    )
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
            world.getPlayers().forEach(p -> {
                worldConfig.updateGameModeIfNeeded(config, p);
            });
        }
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }

    @Completer(command = "world config gamemode <world:managedworlds> <gamemode>")
    public List<String> completeConfigGameMode(final CompletionData data) {
        return Arrays.stream(GameMode.values())
            .map(GameMode::name)
            .filter(s -> s.startsWith(data.getCurrentValue()))
            .collect(Collectors.toList());
    }

    @Executor(
        command = "world config gamerule <world:worlds> <gamerule> <value>",
        description = "Set gamerule of specified world"
    )
    public void executeConfigGameRule(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Please make sure " + worldName + " is loaded!");
            return;
        }
        final String gamerule = data.get("gamerule");
        if (!world.isGameRule(gamerule)) {
            sender.sendMessage(ChatColor.RED + gamerule + " is invalid gamerule!");
            return;
        }
        world.setGameRuleValue(gamerule, data.get("value"));
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }

    @Executor(
        command = "world config keepspawninmemory <world:managedworlds> <value>",
        description = "Set wether to keep spawn in memory"
    )
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

    @Executor(
        command = "world config time <world:worlds> <time>",
        description = "Set time of specified world"
    )
    public void executeConfigTime(final ExecutionData data) {
        final CommandSender sender = data.getSender();
        final String worldName = data.get("world");
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World: " + worldName + " not found");
            return;
        }
        try {
            world.setTime(Integer.parseInt(data.get("time")));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Updated!");
    }
}


