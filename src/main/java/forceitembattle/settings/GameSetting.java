package forceitembattle.settings;

import org.bukkit.Material;

import java.util.List;

public enum GameSetting {

    TEAM("Teams", List.of("", "<gray>Toggle whether <dark_aqua>teams <gray>are allowed or not.", "<dark_gray><i>Only toggleable if 4 or more players are playing!</i>", ""), "isTeamGame", false, Material.RED_BED),
    TEAM_CHAT("Team chat", List.of("", "<gray>Toggle whether <dark_aqua>team chat <gray>is enabled or not.", ""), "isTeamChat", false, Material.FILLED_MAP),
    KEEP_INVENTORY("Keep Inventory", List.of("", "<gray>Toggle whether to <dark_aqua>keep items in inventory <gray>when you die or not.", ""), "keepinventory", true, Material.TOTEM_OF_UNDYING),
    FOOD("Food", List.of("", "<gray>Toggle whether you should <dark_aqua>lose hunger <gray>while playing.", ""), "food", true, Material.COOKED_BEEF),
    BACKPACK("Backpack", List.of("", "<gray>Toggle whether you can use <dark_aqua>backpacks<gray>.", ""), "backpack", true, Material.BUNDLE),
    BACKPACKSIZE("Backpack rows", List.of("", "<gray>Changes size of <dark_aqua>backpack<gray>.", ""), "backpackRows", 3, Material.BUNDLE),
    PVP("PvP", List.of("", "<gray>Toggle whether <dark_aqua>PvP <gray>should be enabled or not.", ""), "pvp", false, Material.IRON_SWORD),
    HARD("Hard", List.of("", "<gray>Toggle whether <dark_aqua>hard items & nether <gray>should be enabled.", ""), "hard", true, Material.NETHERRACK),
    EXTREME("Extreme", List.of("", "<gray>Toggle whether the <dark_aqua>extreme version <gray>should be played.", ""), "extreme", true, Material.BEACON),
    END("End", List.of("", "<gray>Toggle whether <dark_aqua>end <gray>should be accessible.", ""), "end", true, Material.END_STONE),
    FASTER_RANDOM_TICK("Faster plants growth & decay", null, "fasterRandomTick", false, Material.CACTUS),
    POSITIONS("Positions - /pos", List.of("", "<gray>Toggle whether <dark_aqua>positions <gray>can be set.", ""), "positions", true, Material.LIME_WOOL),
    TRADING("Player trading", List.of("", "<gray>Toggle whether players can <dark_aqua>trade items <gray>between each other.", ""), "trading", false, Material.EMERALD),
    TRADING_COOLDOWN("Player trading cooldown", List.of("", "<gray>Changes cooldown of <dark_aqua>trading<gray>.", ""), "tradingCooldown", 3, Material.EMERALD),
    ELYTRA("Elytra gliding", List.of("", "<gray>Prevents gliding with an elytra<gray>.", ""), "elytraGliding", true, Material.ELYTRA),
    CHAIN("Force Chain", List.of("", "<gray>Shows the next forced item.", ""), "forceChain", false, Material.CHAIN),
    STATS("Stats", List.of("", "<gray>Toggle whether this round is played with <dark_aqua>stats<gray>.", ""), "stats", true, Material.WRITABLE_BOOK),
    EVENT("Event", List.of(
            "",
            "<gray>Toggle <dark_aqua>event<gray> modifiers.",
            "<gray>Some commands are OP only",
            "<gray>Keep inventory is force enabled for 5 minutes",
            "",
            ""
    ), "eventModifiers", false, Material.CAKE),
    HARDER_TRACKERS("Harder trackers", List.of("", "<gray>Make tracker recipes harder<gray>.", ""), "hardTrackers", false, Material.KNOWLEDGE_BOOK),

    ;

    private final String displayName;
    private final List<String> descriptionLore;
    private final String configPath;
    private final Object defaultValue;
    private final Material defaultMaterial;

    GameSetting(String displayName, List<String> descriptionLore, String configPath, Object defaultValue, Material defaultMaterial) {
        this.displayName = displayName;
        this.descriptionLore = descriptionLore;
        this.configPath = "settings." +configPath;
        this.defaultValue = defaultValue;
        this.defaultMaterial = defaultMaterial;
    }

    public String displayName() {
        return displayName;
    }
    public List<String> descriptionLore() {
        return descriptionLore;
    }
    public String configPath() {
        return configPath;
    }
    public Object defaultValue() {
        return defaultValue;
    }
    public Material defaultMaterial() {
        return defaultMaterial;
    }
}
