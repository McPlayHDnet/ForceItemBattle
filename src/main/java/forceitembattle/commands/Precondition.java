package forceitembattle.commands;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.settings.GameSetting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Something that must hold before a {@link CustomCommand}'s body runs.
 *
 * <p>A command declares these in {@code preconditions()} and is only invoked when they all hold —
 * replacing a {@code protected boolean requireOp(Player)} that every subclass had to remember to
 * call, remember to invert correctly, and remember to act on. One of the thirteen sites got the
 * inversion wrong and ran its whole body for non-ops only; that is the failure this type exists to
 * make unrepresentable.
 *
 * <h2>Rules that live here and nowhere else</h2>
 *
 * <ul>
 *   <li><b>The console.</b> {@link #OP} and {@link #OP_WHEN_EVENT} pass automatically for a
 *       non-player sender, because the console is implicitly op. Phase and setting gates evaluate
 *       normally. {@link #PARTICIPANT} refuses: the console holds no roster entry.</li>
 *   <li><b>Order matters.</b> {@code preconditions()} returns a {@code List}, evaluated in order,
 *       and the <em>first</em> failure is what the sender is told. Each command keeps the order its
 *       hand-written checks had, so migrating changed no wording.</li>
 *   <li><b>Subcommand gates are not here.</b> {@code CommandAchievement} and {@code CommandStats}
 *       gate individual subcommands off {@code args[0]}, which a command-level declaration cannot
 *       express; they use {@link CustomCommand#requireOp(Player, Runnable)} instead. Making
 *       subcommands first-class is a larger change and deliberately was not taken.</li>
 * </ul>
 *
 * @see CommandContext
 */
public sealed interface Precondition {

    /** Short identifier, for the pinned declaration table in the tests. */
    String label();

    /** Whether this holds for {@code sender} right now. */
    boolean holds(CommandSender sender, CommandContext context);

    /** What the sender is told when it does not. MiniMessage. */
    String refusal();

    /**
     * The same precondition with different wording.
     *
     * <p>Exists because the same condition reads differently in different commands: {@code /pause}
     * refuses a stopped round with "The timer is already paused", where {@code /skip} says "The
     * game is not running. Start it first with /start". Both are better than one generic line.
     */
    default Precondition refusing(String refusal) {
        return new Reworded(this, refusal);
    }

    String NO_PERMISSION = "<red>You don't have permission to use this command.";

    /** Op-only. Passes automatically for the console. */
    Precondition OP = new Named(
            "OP",
            (sender, context) -> !(sender instanceof Player player) || player.isOp(),
            NO_PERMISSION);

    /**
     * Op-only while the EVENT setting is on, open otherwise.
     *
     * <p>The one conditional gate in the codebase, and it appears three times with the same
     * setting: {@code /pause}, {@code /resume} and {@code /pos}. CLAUDE.md describes EVENT as
     * "some commands are OP only", which is exactly this.
     */
    Precondition OP_WHEN_EVENT = new Named(
            "OP_WHEN_EVENT",
            (sender, context) -> !context.settingEnabled(GameSetting.EVENT)
                    || !(sender instanceof Player player)
                    || player.isOp(),
            NO_PERMISSION);

    /** Play is live: the clock ticks and a find counts. Excludes a pause. */
    Precondition ROUND_RUNNING = new Named(
            "ROUND_RUNNING",
            (sender, context) -> context.roundPhase().roundRunning(),
            "<red>The game is not running. Start it first with /start");

    /** Before the round runs, so the roster is not yet frozen. */
    Precondition PRE_GAME = new Named(
            "PRE_GAME",
            (sender, context) -> context.roundPhase().isPreGame(),
            "<red>The game already started");

    /** The round is paused. */
    Precondition PAUSED = new Named(
            "PAUSED",
            (sender, context) -> context.roundPhase().isPausedGame(),
            "<red>The game is not paused.");

    /**
     * The sender holds a place in this round and is not spectating.
     *
     * <p>Absent and spectating are one answer — see {@code CONTEXT.md § Roster} for why someone who
     * joined mid-round holds no entry at all.
     */
    Precondition PARTICIPANT = new Named(
            "PARTICIPANT",
            (sender, context) -> {
                if (!(sender instanceof Player player)) {
                    return false;
                }
                ForceItemPlayer entry = context.entryFor(player.getUniqueId());
                return entry != null && !entry.isSpectator();
            },
            "<red>You are not playing.");

    /** This setting is on for the round being played. */
    static Precondition setting(GameSetting setting, String refusal) {
        return new Named("setting(" + setting.name() + ")",
                (sender, context) -> context.settingEnabled(setting), refusal);
    }

    /** Evaluated for a sender, so a lambda can stay a lambda without naming the pair. */
    @FunctionalInterface
    interface Test {
        boolean holds(CommandSender sender, CommandContext context);
    }

    record Named(String label, Test test, String refusal) implements Precondition {
        @Override
        public boolean holds(CommandSender sender, CommandContext context) {
            return this.test.holds(sender, context);
        }
    }

    record Reworded(Precondition delegate, String refusal) implements Precondition {
        @Override
        public String label() {
            return this.delegate.label();
        }

        @Override
        public boolean holds(CommandSender sender, CommandContext context) {
            return this.delegate.holds(sender, context);
        }
    }
}
