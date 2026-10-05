package forceitembattle.settings;

import javax.annotation.Nullable;

/** Reads go through to the {@link ConfigSource} every time, and {@link #pathFor} is the only place a path is built. */
public final class Ruleset {

    private final ConfigSource config;

    /** The preset this round is being played on, or {@code null} for the top-level settings. */
    @Nullable
    private GamePreset preset;

    public Ruleset(ConfigSource config) {
        this.config = config;
    }

    /** Called on every /start, including ones naming no preset, so a preset can't outlive its round. */
    public void usePreset(@Nullable GamePreset preset) {
        this.preset = preset;
    }

    @Nullable
    public GamePreset preset() {
        return this.preset;
    }

    public boolean enabled(GameSetting setting) {
        return this.config.getBoolean(this.pathFor(setting));
    }

    public int value(GameSetting setting) {
        return this.config.getInt(this.pathFor(setting));
    }

    public void setEnabled(GameSetting setting, boolean enabled) {
        this.config.set(this.pathFor(setting), enabled);
        this.config.save();
    }

    public void setValue(GameSetting setting, int value) {
        this.config.set(this.pathFor(setting), value);
        this.config.save();
    }

    /** A setting's value in a named preset, regardless of which one is active. */
    public boolean enabledIn(GamePreset gamePreset, GameSetting setting) {
        return this.config.getBoolean(pathIn(gamePreset, setting));
    }

    /** Package-private so the tests can state the mapping outright. */
    String pathFor(GameSetting setting) {
        return this.preset == null ? setting.configPath() : pathIn(this.preset, setting);
    }

    private static String pathIn(GamePreset gamePreset, GameSetting setting) {
        return "presets." + gamePreset.getPresetName() + "." + setting.configPath();
    }
}
