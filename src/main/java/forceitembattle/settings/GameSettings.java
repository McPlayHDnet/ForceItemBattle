package forceitembattle.settings;

import forceitembattle.ForceItemBattle;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The plugin's configuration: loading it, the preset catalogue, and the Bukkit side effects two
 * settings carry.
 *
 * <p>Which value a setting has is not decided here — that is {@link Ruleset}, which owns the
 * active preset and therefore the path every read and write resolves to. See
 * {@code CONTEXT.md § Ruleset}.
 */
public class GameSettings {

    private final ForceItemBattle plugin;

    /**
     * The settings in force for the current round. Public so {@code /start} can point it at a
     * preset; everything else goes through the delegating accessors below.
     */
    @Getter
    private final Ruleset ruleset;

    private final ConcurrentSkipListMap<String, GamePreset> gamePresetMap;

    public GameSettings(ForceItemBattle plugin) {
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

                // Read each setting out of the preset section by its own path. This used to
                // compare configPath() against the bare keys under `settings:` and so never
                // matched once -- harmless only because nothing reads this list for a loaded
                // preset today, and not harmless at all the moment an edit-an-existing-preset
                // flow exists.
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

    /**
     * Writes a setting, and applies the two that are also world state.
     *
     * <p>The gamerule side effects stay here rather than moving into {@link Ruleset}: they are
     * Bukkit, and keeping them out is what lets the value rules be read without a server. The write
     * itself now lands wherever {@link #isSettingEnabled} would read from — see the note on
     * {@link Ruleset#pathFor} for the asymmetry that used to make this a no-op during a preset
     * round.
     */
    public void setSettingEnabled(GameSetting gameSetting, boolean enabled) {
        if (gameSetting == GameSetting.KEEP_INVENTORY)
            Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRules.KEEP_INVENTORY, enabled));

        if (gameSetting == GameSetting.FASTER_RANDOM_TICK)
            // 3 is the default random tick speed. 40 is much faster version
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

    /**
     * Read through {@link #getQuickieMode()}, which resolves the active preset, so the write has to
     * as well — see the note on {@link Ruleset#pathFor}.
     */
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
