package forceitembattle.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.ForceItemBattle;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link Ruleset}: which path a setting resolves to, and that reads and writes agree on it.
 *
 * <p>None of this was reachable before. Deciding a setting's value meant
 * {@code plugin.getGamemanager().currentGamePreset()}, so the settings could not be built without
 * the game manager and the game manager could not be built without the settings. Eighty-five call
 * sites read a setting and not one of them could be exercised.
 */
class RulesetTest {

    private static final String TEAM_PATH = "settings.isTeamGame";
    private static final String ROWS_PATH = "settings.backpackRows";

    private static GamePreset preset(String name) {
        GamePreset preset = new GamePreset();
        preset.setPresetName(name);
        return preset;
    }

    // --- which path ------------------------------------------------------------------------

    @Test
    void withoutAPresetASettingIsTopLevel() {
        MapConfigSource config = new MapConfigSource().with(TEAM_PATH, true);
        Ruleset ruleset = new Ruleset(config);

        assertEquals(TEAM_PATH, ruleset.pathFor(GameSetting.TEAM));
        assertTrue(ruleset.enabled(GameSetting.TEAM));
    }

    @Test
    void withAPresetASettingIsUnderThatPreset() {
        MapConfigSource config = new MapConfigSource()
                .with(TEAM_PATH, false)
                .with("presets.speedrun." + TEAM_PATH, true);
        Ruleset ruleset = new Ruleset(config);
        ruleset.usePreset(preset("speedrun"));

        assertEquals("presets.speedrun." + TEAM_PATH, ruleset.pathFor(GameSetting.TEAM));
        assertTrue(ruleset.enabled(GameSetting.TEAM),
                "the preset's value wins over the top-level one");
    }

    /**
     * Defect A. The preset was set by {@code /start &lt;preset&gt;} and never cleared, so the next
     * round played without one silently inherited it. Only visible on a server that plays two
     * rounds in one session — which production does not, because {@code scheduleReset} restarts the
     * JVM, and which the round-test harness does on every run.
     */
    @Test
    void aPresetDoesNotOutliveItsRound() {
        MapConfigSource config = new MapConfigSource()
                .with(TEAM_PATH, false)
                .with("presets.speedrun." + TEAM_PATH, true);
        Ruleset ruleset = new Ruleset(config);

        ruleset.usePreset(preset("speedrun"));
        assertTrue(ruleset.enabled(GameSetting.TEAM));

        ruleset.usePreset(null);
        assertNull(ruleset.preset());
        assertFalse(ruleset.enabled(GameSetting.TEAM),
                "a round started without a preset reads the top-level settings again");
    }

    // --- reads and writes agree ------------------------------------------------------------

    @Test
    void aToggleWithoutAPresetIsReadBack() {
        MapConfigSource config = new MapConfigSource().with(TEAM_PATH, false);
        Ruleset ruleset = new Ruleset(config);

        ruleset.setEnabled(GameSetting.TEAM, true);

        assertTrue(ruleset.enabled(GameSetting.TEAM));
    }

    /**
     * Defect B, and the reason {@link Ruleset#pathFor} is the only place a path is built.
     *
     * <p>Reads resolved through the active preset; writes always went to the top-level path. So
     * during a preset round the settings menu read the preset's value, wrote somewhere nothing
     * looked, and redrew the value it started with — a dead button with no feedback.
     */
    @Test
    void aToggleDuringAPresetRoundIsReadBack() {
        MapConfigSource config = new MapConfigSource()
                .with(TEAM_PATH, false)
                .with("presets.speedrun." + TEAM_PATH, false);
        Ruleset ruleset = new Ruleset(config);
        ruleset.usePreset(preset("speedrun"));

        ruleset.setEnabled(GameSetting.TEAM, true);

        assertTrue(ruleset.enabled(GameSetting.TEAM), "the toggle has to take effect");
        assertEquals(true, config.contents().get("presets.speedrun." + TEAM_PATH),
                "and it has to land where the read looks");
        assertEquals(false, config.contents().get(TEAM_PATH),
                "the top-level setting is not what this round is playing on");
    }

    @Test
    void aValueSettingRoundTripsThroughThePreset() {
        MapConfigSource config = new MapConfigSource();
        Ruleset ruleset = new Ruleset(config);
        ruleset.usePreset(preset("chaos"));

        ruleset.setValue(GameSetting.BACKPACKSIZE, 5);

        assertEquals(5, ruleset.value(GameSetting.BACKPACKSIZE));
        assertEquals(5, config.contents().get("presets.chaos." + ROWS_PATH));
    }

    @Test
    void everyWriteIsFlushed() {
        MapConfigSource config = new MapConfigSource();
        Ruleset ruleset = new Ruleset(config);

        ruleset.setEnabled(GameSetting.TEAM, true);
        ruleset.setValue(GameSetting.BACKPACKSIZE, 4);

        assertEquals(2, config.saveCount());
    }

    /**
     * Deliberate: resolving every setting once at round start would stop an op's mid-round toggle
     * taking effect, which is live behaviour.
     */
    @Test
    void readsAreLiveRatherThanResolvedOnce() {
        MapConfigSource config = new MapConfigSource().with(TEAM_PATH, false);
        Ruleset ruleset = new Ruleset(config);

        assertFalse(ruleset.enabled(GameSetting.TEAM));
        config.set(TEAM_PATH, true);
        assertTrue(ruleset.enabled(GameSetting.TEAM));
    }

    @Test
    void aPresetCanBeAskedDirectlyWhicheverIsActive() {
        MapConfigSource config = new MapConfigSource()
                .with("presets.chaos." + TEAM_PATH, true)
                .with("presets.speedrun." + TEAM_PATH, false);
        Ruleset ruleset = new Ruleset(config);
        ruleset.usePreset(preset("speedrun"));

        assertTrue(ruleset.enabledIn(preset("chaos"), GameSetting.TEAM));
        assertFalse(ruleset.enabled(GameSetting.TEAM));
    }

    @Test
    void thePresetInForceIsReadableBack() {
        Ruleset ruleset = new Ruleset(new MapConfigSource());
        GamePreset chaos = preset("chaos");

        ruleset.usePreset(chaos);

        assertSame(chaos, ruleset.preset());
    }

    /** Reaching for the plugin here restores the dependency cycle, and nothing else would fail. */
    @Test
    void isBuiltFromAConfigSourceRatherThanThePlugin() {
        List<Class<?>> parameters =
                List.of(Ruleset.class.getDeclaredConstructors()[0].getParameterTypes());

        assertEquals(List.of(ConfigSource.class), parameters);
        assertFalse(parameters.contains(ForceItemBattle.class));
    }
}
