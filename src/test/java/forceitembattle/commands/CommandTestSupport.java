package forceitembattle.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import forceitembattle.model.GameState;
import forceitembattle.model.RoundPhase;
import forceitembattle.model.Roster;
import forceitembattle.settings.ConfigSource;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.Ruleset;
import java.util.HashMap;
import java.util.Map;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The three things every command test needs, in one place.
 *
 * <p>A command's observable output is chat, and a screen is a dozen separate {@code sendMessage}
 * calls with blank spacers between them — so no single {@code nextMessage()} is the answer to
 * "what was the player told". {@link #screenOf} drains the queue into one blob and every assertion
 * runs against that. It keeps the legacy section signs, which is deliberate: they are where the
 * text actually breaks, and an assertion that spans a colour change is one that will fail for a
 * reason that has nothing to do with the command.
 *
 * <p>{@link #contextWith} is the other half. A {@link Precondition} asks a {@link CommandContext}
 * three questions, one of which is "is this setting on", so a test of any gated command needs a
 * {@link Ruleset} — over a map here, never a {@code config.yml}.
 *
 * <p>Not a base class. These are static because the command tests differ in almost everything else
 * they set up, and inheriting a fixture that half of them override is how a test file stops being
 * readable on its own.
 */
final class CommandTestSupport {

    private CommandTestSupport() {
    }

    /** Everything the player has been told since this was last called, as one blob. */
    static String screenOf(PlayerMock player) {
        StringBuilder said = new StringBuilder();
        String line;
        while ((line = player.nextMessage()) != null) {
            said.append(line).append('\n');
        }
        return said.toString();
    }

    /** Drains the player's messages and asserts {@code expected} appears among them. */
    static void assertSaid(PlayerMock player, String expected) {
        String said = screenOf(player);
        assertTrue(said.contains(expected),
                "expected to be told '" + expected + "' but got:\n" + said);
    }

    /** Drains the player's messages and asserts {@code unexpected} does not appear. */
    static void assertNotSaid(PlayerMock player, String unexpected) {
        String said = screenOf(player);
        assertFalse(said.contains(unexpected),
                "was not meant to be told '" + unexpected + "' but got:\n" + said);
    }

    /** A context in {@code state}, over {@code roster}, with exactly these settings on. */
    static CommandContext contextWith(GameState state, Roster roster, GameSetting... enabled) {
        RoundPhase phase = new RoundPhase();
        phase.moveTo(state);
        return new CommandContext(phase, ruleset(enabled), roster);
    }

    /** A ruleset over a plain map, so a test can say which settings are on. */
    static Ruleset ruleset(GameSetting... enabled) {
        Map<String, Object> values = new HashMap<>();
        for (GameSetting setting : enabled) {
            values.put(setting.configPath(), true);
        }
        return new Ruleset(new MapConfig(values));
    }

    private record MapConfig(Map<String, Object> values) implements ConfigSource {
        @Override
        public boolean getBoolean(String path) {
            return Boolean.TRUE.equals(this.values.get(path));
        }

        @Override
        public int getInt(String path) {
            Object value = this.values.get(path);
            return value instanceof Integer number ? number : 0;
        }

        @Override
        public void set(String path, Object value) {
            this.values.put(path, value);
        }

        @Override
        public void save() {
        }
    }
}
