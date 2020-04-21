package xyz.shirokuro.worlds;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public final class Bootstrap extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            WorldsPlugin.init(this);
        } catch (PluginInitException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize the plugin.", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
}
