package forceitembattle.manager;

import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.JokerSpend;
import forceitembattle.model.Roster;
import forceitembattle.model.SkipVote;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * The chat and the clock around a {@link SkipVote}.
 *
 * <p>The tally, the quorum and the tie-break are the vote's; this broadcasts, runs the sixty-second
 * task, and applies what the vote decided — the joker charge and the skip. Splitting them is what
 * made the rules testable, and closed the spectator hole in the quorum on the way.
 */
public class VoteSkipManager implements Manager {

    private static final long VOTE_DURATION_TICKS = 20L * 60L;

    private final Roster roster;
    private final ForceItemAssignment assignment;
    private final GameSettings settings;
    private final ItemDifficultiesManager itemDifficultiesManager;
    private final SkipVote vote = new SkipVote();

    private BukkitTask voteTask;
    private ForceItemPlayer initiator;

    public VoteSkipManager(Roster roster, ForceItemAssignment assignment, GameSettings settings,
                           ItemDifficultiesManager itemDifficultiesManager) {
        this.roster = roster;
        this.assignment = assignment;
        this.settings = settings;
        this.itemDifficultiesManager = itemDifficultiesManager;
    }

    @Override
    public void disable() {
        if (this.voteTask != null) {
            this.voteTask.cancel();
            this.voteTask = null;
        }
    }

    public boolean isVoteInProgress() {
        return this.vote.isOpen();
    }

    public void startVoting(Player initiator) {
        ForceItemPlayer starter = this.roster.get(initiator.getUniqueId());
        if (starter == null) {
            // Not in the round, so there is no item of theirs to vote on. /voteskip already
            // refuses this; the guard is here because the vote state is set below and a throw
            // half-way would leave the vote stuck open for the rest of the round.
            return;
        }

        this.initiator = starter;
        this.vote.open(initiator.getUniqueId(), starter.activeMaterial(), participants());

        String materialName = CustomMaterials.nameOf(this.vote.material());
        String unicodeMaterial = this.itemDifficultiesManager.getUnicodeFromMaterial(true, this.vote.material());

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>A skip voting has been started by <green>" + initiator.getName() + "<gray>."));
            player.sendMessage(Text.of("  <dark_gray>● <gray>Duration <dark_gray>» <gold>60 seconds"));
            player.sendMessage(Text.of("  <dark_gray>● <gray>Item <dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName));
            player.sendMessage(" ");
            player.sendMessage(Text.of("                  <dark_gray>[<green><b><click:run_command:'/vote yes'>YES</click></b><dark_gray>]          <dark_gray>[<red><b><click:run_command:'/vote no'>NO</click></b><dark_gray>]"));
            player.sendMessage(" ");
        });

        this.voteTask = Scheduler.runLaterSync(this::endVoting, VOTE_DURATION_TICKS);
    }

    /**
     * Who the vote belongs to: everyone playing when it opened. Spectators are excluded — they used
     * to both inflate the quorum and be able to fill it.
     */
    private Set<UUID> participants() {
        return this.roster.players().entrySet().stream()
                .filter(entry -> Roster.isPlaying(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void castVote(Player player, boolean voteYes) {
        switch (this.vote.cast(player.getUniqueId(), voteYes)) {
            case ALREADY_VOTED -> player.sendMessage(Text.of("<red>You have already voted."));
            case NOT_ELIGIBLE -> player.sendMessage(
                    Text.of("<red>Only players in the round can vote."));
            case NO_VOTE_OPEN -> player.sendMessage(Text.of("<red>There is no vote running."));
            case COUNTED -> confirm(player, voteYes);
            case CLOSES_THE_VOTE -> {
                confirm(player, voteYes);
                if (this.voteTask != null) {
                    this.voteTask.cancel();
                }
                this.endVoting();
            }
        }
    }

    private static void confirm(Player player, boolean voteYes) {
        player.sendMessage(voteYes
                ? Text.of("<gray>You voted for <green><b>YES</b><gray>!")
                : Text.of("<gray>You voted for <red><b>NO</b><gray>!"));
    }

    public void endVoting() {
        Material votedMaterial = this.vote.material();
        SkipVote.Tally tally = this.vote.close();

        String voteLabel = (tally.yes() != 1 ? "votes" : "vote");
        String materialName = CustomMaterials.nameOf(votedMaterial);
        String unicodeMaterial = this.itemDifficultiesManager.getUnicodeFromMaterial(true, votedMaterial);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>The skip voting has been ended."));
            player.sendMessage(Text.of("  <dark_gray>● <green><b>YES</b> <dark_gray>» <gold>" + tally.yes() + " " + voteLabel));
            player.sendMessage(Text.of("  <dark_gray>● <red><b>NO</b> <dark_gray>» <gold>" + tally.no() + " " + voteLabel));
            player.sendMessage(" ");
            if (tally.tie()) {
                player.sendMessage(Text.of("<gray>It was a tie! Choosing randomly..."));
            }
            player.sendMessage(Text.of("<dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName + " <gray>is " + (tally.carried() ? "now" : "not") + " skipped."));
            player.sendMessage(" ");
        });

        // The vote costs the initiator a joker whether or not it carried — and the button has to
        // agree. It used to charge the pool and leave the stack alone, so the initiator's hotbar read
        // one too high until /fixskips ran on their next respawn and quietly repaired it.
        Player initiatorPlayer = this.initiator.player();
        PlayerOutfitter.setJokerStack(initiatorPlayer,
                JokerSpend.charge(this.initiator, PlayerOutfitter.jokerStackIn(initiatorPlayer)));
        if (tally.carried()) {
            this.assignment.skipAll(this.initiator, this.settings.isSettingEnabled(GameSetting.RUN));
        }

        this.voteTask = null;
    }

    public void cancelVote() {
        if (this.voteTask != null) {
            this.voteTask.cancel();
        }
        this.vote.cancel();
        this.initiator = null;
        this.voteTask = null;
    }
}
