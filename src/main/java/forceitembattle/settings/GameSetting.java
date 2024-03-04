package forceitembattle.settings;

import org.bukkit.Material;

import javax.annotation.Nullable;

public enum GameSetting {

    TEAM("Teams", "isTeamGame", false, Material.RED_BED, 33),
    KEEP_INVENTORY("Keep Inventory", "keepinventory", true, Material.TOTEM_OF_UNDYING, 21),
    FOOD("Food", "food", true, Material.COOKED_BEEF, 20),
    BACKPACK("Backpack", "backpack", true, Material.BUNDLE, 31),
    PVP("PvP", "pvp", false, Material.IRON_SWORD, 28),
    NETHER("Nether", "nether", true, Material.NETHERRACK, 23),
    EXTREME("Extreme", "extreme", true, Material.END_STONE, 24),
    FASTER_RANDOM_TICK("Faster plants growth & decay", "fasterRandomTick", false, Material.CACTUS, 29),
    STATS("Stats", "stats", true, Material.WRITABLE_BOOK, 34);

    private final String displayName;
    private final String configPath;
    private final boolean defaultValue;
    private final Material defaultMaterial;
    private final int defaultSlot;

    GameSetting(String displayName, String configPath, boolean defaultValue, Material defaultMaterial, int defaultSlot) {
        this.displayName = displayName;
        this.configPath = "settings." +configPath;
        this.defaultValue = defaultValue;
        this.defaultMaterial = defaultMaterial;
        this.defaultSlot = defaultSlot;
    }

    public String displayName() {
        return displayName;
    }

    public String configPath() {
        return configPath;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public Material defaultMaterial() {
        return defaultMaterial;
    }

    public int defaultSlot() {
        return defaultSlot;
    }
}
