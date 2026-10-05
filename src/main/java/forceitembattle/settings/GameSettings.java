package forceitembattle.settings;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/** Loading, the preset catalogue and the gamerule side effects; which value a setting has is {@link Ruleset}'s. */
public class GameSettings {

    private final JavaPlugin plugin;

    /** Public so {@code /start} can point it at a preset. */
    @Getter
    private final Ruleset ruleset;

    private final ConcurrentSkipListMap<String, GamePreset> gamePresetMap;

    public GameSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ruleset = new Ruleset(new BukkitConfigSource(plugin));
        this.gamePresetMap = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

        this.plugin.getConfig().addDefault("timer.time", 0);

        for (GameSetting gameSettings : GameSetting.values()) {
            this.plugin.getConfig().addDefault(gameSettings.configPath(), gameSettings.defaultValue());
        }

        this.plugin.getConfig().addDefault("standard.countdown", 30);
        this.plugin.getConfig().addDefault("standard.jokers", 3);

        if (!this.plugin.getConfig().isConfigurationSection("presets")) {
            this.plugin.getConfig().createSection("presets");
        }

        ConfigurationSection presets = this.plugin.getConfig().getConfigurationSection("presets");
        if (presets != null) {
            presets.getKeys(false).forEach(keys -> {
                ConfigurationSection configurationSection = presets.getConfigurationSection(keys);
                if (configurationSection == null) {
                    return;
                }
                GamePreset gamePreset = new GamePreset();
                gamePreset.setPresetName(keys);
                gamePreset.setCountdown(configurationSection.getInt("countdown"));
                gamePreset.setJokers(configurationSection.getInt("jokers"));
                gamePreset.setBackpackRows(configurationSection.getInt("backpackRows"));

                // By configPath(), not the bare keys under `settings:` — those never match.
                gamePreset.getGameSettings().clear();
                for (GameSetting gameSetting : GameSetting.values()) {
                    if (gameSetting.defaultValue() instanceof Boolean
                            && configurationSection.getBoolean(gameSetting.configPath())) {
                        gamePreset.getGameSettings().add(gameSetting);
                    }
                }
                this.gamePresetMap.put(keys, gamePreset);
            });
        }

    }

    public boolean isSettingEnabledInPreset(GamePreset gamePreset, GameSetting gameSetting) {
        return this.ruleset.enabledIn(gamePreset, gameSetting);
    }

    public boolean isSettingEnabled(GameSetting gameSetting) {
        return this.ruleset.enabled(gameSetting);
    }

    /** Gamerule side effects stay here so {@link Ruleset} needs no server. */
    public void setSettingEnabled(GameSetting gameSetting, boolean enabled) {
        if (gameSetting == GameSetting.KEEP_INVENTORY)
            Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRules.KEEP_INVENTORY, enabled));

        if (gameSetting == GameSetting.FASTER_RANDOM_TICK)
            // 3 is vanilla's random tick speed.
            Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRules.RANDOM_TICK_SPEED, enabled ? 40 : 3));

        this.ruleset.setEnabled(gameSetting, enabled);
    }

    public void setSettingValue(GameSetting gameSetting, Integer value) {
        if (gameSetting.defaultValue() instanceof Integer) {
            this.ruleset.setValue(gameSetting, value);
        }
    }

    public int getSettingValue(GameSetting gameSetting) {
        return this.ruleset.value(gameSetting);
    }

    public QuickieMode getQuickieMode() {
        return QuickieMode.fromOrdinal(this.getSettingValue(GameSetting.QUICKIE));
    }

    /** Read through {@link #getQuickieMode()}, which resolves the active preset, so this must too. */
    public void setQuickieMode(QuickieMode quickieMode) {
        this.ruleset.setValue(GameSetting.QUICKIE, quickieMode.ordinal());
    }

    public void addGamePreset(GamePreset gamePreset) {
        ConfigurationSection configurationSection = this.plugin.getConfig().getConfigurationSection("presets");

        if (configurationSection != null) {
            ConfigurationSection presetSection = configurationSection.createSection(gamePreset.getPresetName());

            presetSection.set("countdown", gamePreset.getCountdown());
            presetSection.set("jokers", gamePreset.getJokers());
            presetSection.set("backpackRows", gamePreset.getBackpackRows());

            for (GameSetting gameSetting : GameSetting.values()) {
                presetSection.set(gameSetting.configPath(), gamePreset.getGameSettings().contains(gameSetting));
            }
        }

        this.plugin.saveConfig();
        this.gamePresetMap.put(gamePreset.getPresetName(), gamePreset);

    }

    public GamePreset getGamePreset(String presetName) {
        return this.gamePresetMap.get(presetName);
    }

    public Map<String, GamePreset> gamePresetMap() {
        return gamePresetMap;
    }
}
