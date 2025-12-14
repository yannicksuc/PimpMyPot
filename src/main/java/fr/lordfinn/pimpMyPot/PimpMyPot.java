package fr.lordfinn.pimpMyPot;

import org.bukkit.plugin.java.JavaPlugin;

public final class PimpMyPot extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PotListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
