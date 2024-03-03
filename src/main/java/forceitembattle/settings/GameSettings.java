package forceitembattle.settings;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.preset.GamePreset;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class GameSettings {

    private final ForceItemBattle plugin;

    private final ConcurrentSkipListMap<String, GamePreset> gamePresetMap;

    public GameSettings(ForceItemBattle plugin) {
        this.plugin = plugin;
        this.gamePresetMap = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

        this.plugin.getConfig().addDefault("timer.time", 0);

        for (GameSetting gameSettings : GameSetting.values()) {
            this.plugin.getConfig().addDefault(gameSettings.configPath(), gameSettings.defaultValue());
        }

        this.plugin.getConfig().addDefault("standard.getCountdown", 30);
        this.plugin.getConfig().addDefault("standard.getJokers", 3);
        this.plugin.getConfig().addDefault("standard.getBackpackSize", 27);

        if (!this.plugin.getConfig().isConfigurationSection("presets")) {
            this.plugin.getConfig().createSection("presets");
        }

        if (this.plugin.getConfig().isConfigurationSection("presets")) {
            this.plugin.getConfig().getConfigurationSection("presets").getKeys(false).forEach(keys -> {
                ConfigurationSection configurationSection = this.plugin.getConfig().getConfigurationSection("presets").getConfigurationSection(keys);
                GamePreset gamePreset = new GamePreset();
                gamePreset.setPresetName(keys);
                gamePreset.setCountdown(configurationSection.getInt("getCountdown"));
                gamePreset.setJokers(configurationSection.getInt("getJokers"));
                gamePreset.setBackpackSize(configurationSection.getInt("getBackpackSize"));

                configurationSection.getConfigurationSection("settings").getKeys(false).forEach(settingKeys -> {
                    for (GameSetting gameSetting : GameSetting.values()) {
                        if (gameSetting.configPath().equals(settingKeys)) {
                            gamePreset.getGameSettings().add(gameSetting);
                        }
                    }

                });
                this.gamePresetMap.put(keys, gamePreset);
            });
        }

    }

    public boolean isSettingEnabledInPreset(GamePreset gamePreset, GameSetting gameSetting) {
        return this.plugin.getConfig().getBoolean("presets." + gamePreset.getPresetName() + "." + gameSetting.configPath());
    }

    public boolean isSettingEnabled(GameSetting gameSetting) {
        if (this.plugin.getGamemanager().currentGamePreset() != null) {
            return this.isSettingEnabledInPreset(this.plugin.getGamemanager().currentGamePreset(), gameSetting);
        }
        return this.plugin.getConfig().getBoolean(gameSetting.configPath());
    }

    public void setSettingEnabled(GameSetting gameSetting, boolean enabled) {
        if (gameSetting == GameSetting.KEEP_INVENTORY)
            Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.KEEP_INVENTORY, enabled));

        if (gameSetting == GameSetting.FASTER_RANDOM_TICK)
            // 3 is the default random tick speed. 40 is much faster version
            Bukkit.getWorlds().forEach(worlds -> worlds.setGameRule(GameRule.RANDOM_TICK_SPEED, enabled ? 40 : 3));


        this.plugin.getConfig().set(gameSetting.configPath(), enabled);
        this.plugin.saveConfig();
    }

    public void addGamePreset(GamePreset gamePreset) {
        ConfigurationSection configurationSection = this.plugin.getConfig().getConfigurationSection("presets");

        if (configurationSection != null) {
            ConfigurationSection presetSection = configurationSection.createSection(gamePreset.getPresetName());

            presetSection.set("getCountdown", gamePreset.getCountdown());
            presetSection.set("getJokers", gamePreset.getJokers());
            presetSection.set("getBackpackSize", gamePreset.getBackpackSize());

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
