package xyz.shirokuro.worlds;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Locale;

public final class WorldCreationConfig {
    private String generatorName;
    private String generatorSettings;
    private World.Environment environment;
    private WorldType worldType;

    public WorldCreationConfig(
            final String generatorName,
            final String generatorSettings,
            final World.Environment environment,
            final WorldType worldType) {
        this.generatorName = generatorName;
        this.generatorSettings = generatorSettings;
        this.environment = environment;
        this.worldType = worldType;
    }

    public static WorldCreationConfig load(final ConfigurationSection section) throws ConfigException {
        final String generatorName = section.getString("generator-name");
        final String generatorSettings = section.getString("generator-settings");
        final String environmentStr = section.getString("environment");
        if (environmentStr == null) {
            throw new ConfigKeyNotPresentException("environment");
        }
        final World.Environment environment;
        try {
            environment = World.Environment.valueOf(environmentStr.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new ConfigException(environmentStr + " is invalid environment name", e);
        }
        final String worldTypeStr = section.getString("world-type");
        if (worldTypeStr == null) {
            throw new ConfigKeyNotPresentException("world-type");
        }
        final WorldType worldType;
        try {
            worldType = WorldType.valueOf(worldTypeStr.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new ConfigException(worldTypeStr + " is invalid world type", e);
        }
        return new WorldCreationConfig(generatorName, generatorSettings, environment, worldType);
    }

    public void fillConfigurationSection(final ConfigurationSection section) {
        section.set("generator-name", generatorName);
        section.set("generator-settings", generatorSettings);
        section.set("environment", environment.name());
        section.set("world-type", worldType.name());
    }

    public void configureWorldCreator(final WorldCreator creator) {
        if (generatorName != null) {
            creator.generator(generatorName);
        }
        if (generatorSettings != null) {
            creator.generatorSettings(generatorSettings);
        }
        creator.environment(environment);
        creator.type(worldType);
        creator.generateStructures(false);
    }

    public String getGeneratorName() {
        return generatorName;
    }

    public void setGeneratorName(final String generatorName) {
        this.generatorName = generatorName;
    }

    public String getGeneratorSettings() {
        return generatorSettings;
    }

    public void setGeneratorSettings(final String generatorSettings) {
        this.generatorSettings = generatorSettings;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(final World.Environment environment) {
        this.environment = environment;
    }

    public WorldType getWorldType() {
        return worldType;
    }

    public void setWorldType(final WorldType worldType) {
        this.worldType = worldType;
    }
}


