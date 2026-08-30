package forceitembattle.settings;

import javax.annotation.Nullable;

/**
 * The settings in force for the round being played, and where each one is read from.
 *
 * <h2>Why this exists</h2>
 *
 * <p>A round is played either on the top-level configuration or on a named {@link GamePreset}, and
 * that choice decides the config path of every setting. {@link GameSettings} used to make it by
 * calling {@code plugin.getGamemanager().currentGamePreset()} on every single read — a cycle
 * between the settings and the game manager, in a codebase where 85 sites read a setting. Neither
 * module could be built without the other, which is why {@code settings/} had no tests and why the
 * plugin reach-through could not be unwound in either direction.
 *
 * <p>The preset was only ever stored on the game manager because {@code /start} happened to be
 * holding one. Which preset is live is a fact about the settings, so it lives here, and the
 * back-edge disappears.
 *
 * <h2>A view, not a snapshot</h2>
 *
 * <p>Reads go through to the {@link ConfigSource} every time rather than being resolved once when
 * the round starts. That is deliberate: an op toggling a setting mid-round takes effect
 * immediately today, and freezing the values would have quietly taken that away. The seam is what
 * buys the testability here — not immutability.
 *
 * <h2>One path, both directions</h2>
 *
 * <p>{@link #pathFor} is the only place a config path is built, and reads and writes both use it.
 * They did not always: reads resolved through the active preset while writes always went to the
 * top-level path, so during a preset round the settings menu displayed the preset's value, wrote
 * somewhere nothing read, and redrew the value it started with. The toggle was dead and said
 * nothing. Keep the two ends on the same path.
 *
 * <p><b>Consequence worth knowing:</b> a toggle during a preset round now edits that saved preset,
 * so it persists into every later round played on it. That is the price of the two ends agreeing;
 * the alternative — an overlay discarded at the end of the round — is a bigger idea than this
 * module needs.
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
     * <p>Called by {@code /start} on every run, including the runs that name no preset — which is
     * the fix for a preset outliving its round. The field was previously set only when a preset was
     * named and never cleared, so {@code /start speedrun} followed by {@code /start 90 3} played
     * the second round on speedrun's settings. Production never saw it because {@code
     * scheduleReset} restarts the JVM between rounds and a fresh manager starts at null; a server
     * that plays two rounds in one session does, which is exactly what the round-test harness does.
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
     *
     * <p>Package-private so the tests can state the mapping outright — it is the whole rule this
     * module encodes, and it was previously only observable by watching which value came back.
     */
    String pathFor(GameSetting setting) {
        return this.preset == null ? setting.configPath() : pathIn(this.preset, setting);
    }

    private static String pathIn(GamePreset gamePreset, GameSetting setting) {
        return "presets." + gamePreset.getPresetName() + "." + setting.configPath();
    }
}
