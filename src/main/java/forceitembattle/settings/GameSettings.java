package forceitembattle.settings;

import forceitembattle.ForceItemBattle;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;

public class GameSettings {

    private final ForceItemBattle plugin;

    public GameSettings(ForceItemBattle plugin) {
        this.plugin = plugin;

        this.plugin.getConfig().addDefault("timer.time", 0);
        this.plugin.getConfig().addDefault("settings.isTeamGame", false);
        this.plugin.getConfig().addDefault("settings.keepinventory", true);
        this.plugin.getConfig().addDefault("settings.food", true);
        this.plugin.getConfig().addDefault("settings.backpack", true);
        this.plugin.getConfig().addDefault("settings.pvp", false);
        this.plugin.getConfig().addDefault("settings.nether", true);
        this.plugin.getConfig().addDefault("settings.end", true);
        this.plugin.getConfig().addDefault("settings.fasterRandomTick", false);

        this.plugin.getConfig().addDefault("standard.countdown", 30);
        this.plugin.getConfig().addDefault("standard.jokers", 3);
        this.plugin.getConfig().addDefault("standard.backpackSize", 27);
    }

    public boolean isNetherEnabled() {
        return plugin.getConfig().getBoolean("settings.nether");
    }

    public void setNetherEnabled(boolean enabled) {
        plugin.getConfig().set("settings.nether", enabled);
        plugin.saveConfig();
    }

    public boolean isEndEnabled() {
        return plugin.getConfig().getBoolean("settings.end", true);
    }

    public void setEndEnabled(boolean enabled) {
        plugin.getConfig().set("settings.end", enabled);
        plugin.saveConfig();
    }

    public boolean isPvpEnabled() {
        return plugin.getConfig().getBoolean("settings.pvp");
    }

    public void setPvpEnabled(boolean enabled) {
        plugin.getConfig().set("settings.pvp", enabled);
        plugin.saveConfig();
    }

    public boolean isFoodEnabled() {
        return plugin.getConfig().getBoolean("settings.food");
    }

    public void setFoodEnabled(boolean enabled) {
        plugin.getConfig().set("settings.food", enabled);
        plugin.saveConfig();
    }

    public boolean isBackpackEnabled() {
        return plugin.getConfig().getBoolean("settings.backpack");
    }

    public void setBackpackEnabled(boolean enabled) {
        plugin.getConfig().set("settings.backpack", enabled);
        plugin.saveConfig();
    }

    public boolean isKeepInventoryEnabled() {
        return plugin.getConfig().getBoolean("settings.keepinventory");
    }

    public void setKeepInventoryEnabled(boolean enabled) {
        Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.KEEP_INVENTORY, enabled));

        plugin.getConfig().set("settings.keepinventory", enabled);
        plugin.saveConfig();
    }

    public boolean isFasterRandomTick() {
        return plugin.getConfig().getBoolean("settings.fasterRandomTick");
    }

    public void  setFasterRandomTick(boolean enabled) {
        // 3 is the default random tick speed. 40 is much faster version
        Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.RANDOM_TICK_SPEED, enabled? 40 : 3));

        plugin.getConfig().set("settings.fasterRandomTick", enabled);
        plugin.saveConfig();
    }

    public boolean isTeamGame() {
        return plugin.getConfig().getBoolean("settings.isTeamGame");
    }

    public void setTeamGame(boolean enabled) {
        plugin.getConfig().set("settings.isTeamGame", enabled);
        plugin.saveConfig();
    }

}
