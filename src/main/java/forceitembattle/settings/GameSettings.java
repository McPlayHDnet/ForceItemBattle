package forceitembattle.settings;

import forceitembattle.ForceItemBattle;

public class GameSettings {

    private final ForceItemBattle plugin;

    public GameSettings(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    public boolean isNetherEnabled() {
        return plugin.getConfig().getBoolean("settings.nether");
    }

    public void setNetherEnabled(boolean enabled) {
        plugin.getConfig().set("settings.nether", enabled);
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
        plugin.getConfig().set("settings.keepinventory", enabled);
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
