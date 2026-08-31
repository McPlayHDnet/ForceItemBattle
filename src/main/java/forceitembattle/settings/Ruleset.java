package forceitembattle.settings;

import javax.annotation.Nullable;

/**
 * The settings in force for the round being played, and where each one is read from: it owns the
 * active {@link GamePreset} and therefore the config path every read and write resolves to. See
 * {@code CONTEXT.md § Ruleset}.
 *
 * <p>Two rules that have each been got wrong once and are worth keeping in view: reads go through
 * to the {@link ConfigSource} every time rather than being snapshotted at {@code /start}, and
 * {@link #pathFor} is the only place a path is built, for reads and writes alike.
 */
public final class Ruleset {

    private final ConfigSource config;

    /** The preset this round is being played on, or {@code null} for the top-level settings. */
    @Nullable
    private GamePreset preset;

    public Ruleset(ConfigSource config) {
        this.config = config;
    }

    /**
     * Points this ruleset at a preset, or back at the top-level settings when given {@code null}.
     *
     * <p>Called by {@code /start} on <em>every</em> run, including the ones that name no preset.
     * That is what stops a preset outliving its round, which only a server playing two rounds in
     * one session — the round-test harness — ever notices.
     */
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

    /** Whether a preset is decided per round, so a setting's value can be asked for one directly. */
    public boolean enabledIn(GamePreset gamePreset, GameSetting setting) {
        return this.config.getBoolean(pathIn(gamePreset, setting));
    }

    public int valueIn(GamePreset gamePreset, GameSetting setting) {
        return this.config.getInt(pathIn(gamePreset, setting));
    }

    /**
     * Where this setting lives right now: under the active preset, or at the top level.
     * Package-private so the tests can state the mapping outright.
     */
    String pathFor(GameSetting setting) {
        return this.preset == null ? setting.configPath() : pathIn(this.preset, setting);
    }

    private static String pathIn(GamePreset gamePreset, GameSetting setting) {
        return "presets." + gamePreset.getPresetName() + "." + setting.configPath();
    }
}
