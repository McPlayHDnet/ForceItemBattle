package forceitembattle.manager;

import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.JokerSpend;
import forceitembattle.model.Roster;
import forceitembattle.model.RoundPhase;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/** Broadcasts, runs the sixty-second task and applies the outcome; the rules are {@link SkipVote}'s. */
public class VoteSkipManager implements Manager {

    private static final int VOTE_DURATION_SECONDS = 60;

    private final Roster roster;
    private final RoundPhase roundPhase;
    private final ForceItemAssignment assignment;
    private final GameSettings settings;
    private final ItemDifficultiesManager itemDifficultiesManager;
    private final SkipVote vote = new SkipVote();

    private BukkitTask voteTask;
    private ForceItemPlayer initiator;
    private int secondsLeft;

    public VoteSkipManager(Roster roster, RoundPhase roundPhase, ForceItemAssignment assignment,
                           GameSettings settings, ItemDifficultiesManager itemDifficultiesManager) {
        this.roster = roster;
        this.roundPhase = roundPhase;
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
            // /voteskip already refuses this, but a throw after the vote state is set would leave the vote stuck open.
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

        this.secondsLeft = VOTE_DURATION_SECONDS;
        this.voteTask = Scheduler.runTimerSync(new BukkitRunnable() {
            @Override
            public void run() {
                tickVote();
            }
        }, 20L, 20L);
    }

    /** The minute counts play time only, like the round clock; a round that ends drops the vote. */
    private void tickVote() {
        if (this.roundPhase.isPausedGame()) {
            return;
        }
        if (!this.roundPhase.roundRunning()) {
            this.cancelVote();
            return;
        }
        if (--this.secondsLeft <= 0) {
            this.endVoting();
        }
    }

    /** Everyone playing when the vote opened; spectators must neither inflate nor fill the quorum. */
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
        if (this.voteTask != null) {
            this.voteTask.cancel();
            this.voteTask = null;
        }

        Material votedMaterial = this.vote.material();
        SkipVote.Tally tally = this.vote.close();

        // Found or skipped while the vote ran: carrying it would skip the item that replaced it.
        if (this.initiator.activeMaterial() != votedMaterial) {
            String name = CustomMaterials.nameOf(votedMaterial);
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(Text.of(
                    "<gray>The skip voting has been ended: <gold>" + name
                            + " <gray>is no longer the item, so nothing is skipped.")));
            return;
        }

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

        // The vote costs the initiator a joker whether or not it carried, and the hotbar stack has to agree.
        Player initiatorPlayer = this.initiator.player();
        PlayerOutfitter.setJokerStack(initiatorPlayer,
                JokerSpend.charge(this.initiator, PlayerOutfitter.jokerStackIn(initiatorPlayer)));
        if (tally.carried()) {
            this.assignment.skipAll(this.initiator, this.settings.isSettingEnabled(GameSetting.RUN));
        }
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
