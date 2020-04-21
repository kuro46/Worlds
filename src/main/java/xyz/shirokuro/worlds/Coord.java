package xyz.shirokuro.worlds;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Objects;

public final class Coord {
    private final double x, y, z;
    private final float pitch, yaw;

    public Coord(final double x, final double y, final double z, final float yaw, final float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static Coord fromLocation(final Location loc) {
        Objects.requireNonNull(loc, "loc");
        return new Coord(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public static Coord fromConfigSection(final ConfigurationSection section) throws ConfigException {
        Objects.requireNonNull(section, "section");
        assertConfigKeys(section, "x", "y", "z", "pitch", "yaw");
        final double x = section.getDouble("x");
        final double y = section.getDouble("y");
        final double z = section.getDouble("z");
        final float pitch = (float) section.getDouble("pitch");
        final float yaw = (float) section.getDouble("yaw");
        return new Coord(x, y, z, yaw, pitch);
    }

    private static void assertConfigKeys(final ConfigurationSection section, final String... keys) throws ConfigKeyNotPresentException {
        for (final String key : keys) {
            if (!section.contains(key)) {
                throw new ConfigKeyNotPresentException(key);
            }
        }
    }

    public void fillConfigSection(final ConfigurationSection section) {
        Objects.requireNonNull(section, "section");
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("pitch", pitch);
        section.set("yaw", yaw);
    }

    public Location withWorld(final World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }
}


