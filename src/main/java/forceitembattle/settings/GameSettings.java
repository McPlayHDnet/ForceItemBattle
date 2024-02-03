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

    public boolean isPvpEnabled() {
        return plugin.getConfig().getBoolean("settings.pvp");
    }

    public boolean isFoodEnabled() {
        return plugin.getConfig().getBoolean("settings.food");
    }

    public boolean isBackpackEnabled() {
        return plugin.getConfig().getBoolean("settings.backpack");
    }

    public boolean isKeepInventoryEnabled() {
        return plugin.getConfig().getBoolean("settings.keepinventory");
    }

    public boolean isTeamGame() {
        return plugin.getConfig().getBoolean("settings.isTeamGame");
    }

}
