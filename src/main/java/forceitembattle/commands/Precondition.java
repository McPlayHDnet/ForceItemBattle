package forceitembattle.commands;

import forceitembattle.model.ForceItemPlayer;
import forceitembattle.settings.GameSetting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Something that must hold before a {@link CustomCommand}'s body runs. A command declares these in
 * {@code preconditions()} and is only invoked when they all hold.
 *
 * <ul>
 *   <li><b>The console.</b> {@link #OP} and {@link #OP_WHEN_EVENT} pass automatically for a
 *       non-player sender, because the console is implicitly op. Phase and setting gates evaluate
 *       normally. {@link #PARTICIPANT} refuses: the console holds no roster entry.</li>
 *   <li><b>Order matters.</b> The list is evaluated in order and the <em>first</em> failure is what
 *       the sender is told.</li>
 *   <li><b>Subcommand gates are not here.</b> {@code CommandAchievement} and {@code CommandStats}
 *       gate individual subcommands off {@code args[0]}, which a command-level declaration cannot
 *       express; they use {@link CustomCommand#requireOp(Player, Runnable)} instead.</li>
 * </ul>
 *
 * @see CommandContext
 */
public sealed interface Precondition {

    /** Short identifier, for the pinned declaration table in the tests. */
    String label();

    boolean holds(CommandSender sender, CommandContext context);

    /** What the sender is told when it does not. MiniMessage. */
    String refusal();

    /** The same precondition with different wording, since one condition reads differently per command. */
    default Precondition refusing(String refusal) {
        return new Reworded(this, refusal);
    }

    String NO_PERMISSION = "<red>You don't have permission to use this command.";

    /** Op-only. Passes automatically for the console. */
    Precondition OP = new Named(
            "OP",
            (sender, context) -> !(sender instanceof Player player) || player.isOp(),
            NO_PERMISSION);

    /** Op-only while the EVENT setting is on, open otherwise. */
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

    Precondition PAUSED = new Named(
            "PAUSED",
            (sender, context) -> context.roundPhase().isPausedGame(),
            "<red>The game is not paused.");

    /**
     * The sender holds a place in this round and is not spectating. Absent and spectating are one
     * answer: someone who joined mid-round holds no roster entry at all.
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
