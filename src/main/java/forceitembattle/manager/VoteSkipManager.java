package forceitembattle.manager;

import forceitembattle.model.Roster;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Scheduler;
import forceitembattle.util.Text;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class VoteSkipManager implements Manager {
    private final Roster roster;
    private final ForceItemAssignment assignment;
    private final GameSettings settings;
    private final ItemDifficultiesManager itemDifficultiesManager;
    private final Set<UUID> yesVotes = new HashSet<>();
    private final Set<UUID> noVotes = new HashSet<>();
    private final Random random = new Random();
    @Getter
    private boolean voteInProgress = false;
    private BukkitTask voteTask;
    private Material votedMaterial;
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

    public void startVoting(Player initiator) {
        ForceItemPlayer starter = this.roster.get(initiator.getUniqueId());
        if (starter == null) {
            // Not in the round, so there is no item of theirs to vote on. /voteskip already
            // refuses this; the guard is here because the vote state is set below and a throw
            // half-way would leave voteInProgress stuck on for the rest of the round.
            return;
        }

        this.voteInProgress = true;
        this.yesVotes.clear();
        this.noVotes.clear();
        this.yesVotes.add(initiator.getUniqueId());
        this.initiator = starter;
        this.votedMaterial = starter.activeMaterial();

        String materialName = CustomMaterials.nameOf(this.votedMaterial);
        String unicodeMaterial = this.itemDifficultiesManager.getUnicodeFromMaterial(true, this.votedMaterial);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>A skip voting has been started by <green>" + initiator.getName() + "<gray>."));
            player.sendMessage(Text.of("  <dark_gray>● <gray>Duration <dark_gray>» <gold>60 seconds"));
            player.sendMessage(Text.of("  <dark_gray>● <gray>Item <dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName));
            player.sendMessage(" ");
            player.sendMessage(Text.of("                  <dark_gray>[<green><b><click:run_command:'/vote yes'>YES</click></b><dark_gray>]          <dark_gray>[<red><b><click:run_command:'/vote no'>NO</click></b><dark_gray>]"));
            player.sendMessage(" ");
        });

        this.voteTask = Scheduler.runLaterSync(this::endVoting, 20 * 60);
    }

    public void castVote(Player player, boolean voteYes) {
        UUID uuid = player.getUniqueId();
        if (this.yesVotes.contains(uuid) || this.noVotes.contains(uuid)) {
            player.sendMessage(Text.of("<red>You have already voted."));
            return;
        }

        if (voteYes) {
            this.yesVotes.add(uuid);
            player.sendMessage(Text.of("<gray>You voted for <green><b>YES</b><gray>!"));
        } else {
            this.noVotes.add(uuid);
            player.sendMessage(Text.of("<gray>You voted for <red><b>NO</b><gray>!"));
        }

        int totalPlayers = this.roster.players().size();
        int totalVotes = this.yesVotes.size() + this.noVotes.size();

        if (totalVotes >= totalPlayers) {
            if (this.voteTask != null) this.voteTask.cancel();
            this.endVoting();
        }
    }

    public void endVoting() {
        this.voteInProgress = false;

        int yes = this.yesVotes.size();
        int no = this.noVotes.size();
        String voteLabel = (yes != 1 ? "votes" : "vote");

        String materialName = CustomMaterials.nameOf(this.votedMaterial);
        String unicodeMaterial = this.itemDifficultiesManager.getUnicodeFromMaterial(true, this.votedMaterial);

        boolean isTie = yes == no;
        boolean skipItem = yes > no || (isTie && random.nextBoolean());

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>The skip voting has been ended."));
            player.sendMessage(Text.of("  <dark_gray>● <green><b>YES</b> <dark_gray>» <gold>" + yes + " " + voteLabel));
            player.sendMessage(Text.of("  <dark_gray>● <red><b>NO</b> <dark_gray>» <gold>" + no + " " + voteLabel));
            player.sendMessage(" ");
            if (isTie) {
                player.sendMessage(Text.of("<gray>It was a tie! Choosing randomly..."));
            }
            player.sendMessage(Text.of("<dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName + " <gray>is " + (skipItem ? "now" : "not") + " skipped."));
            player.sendMessage(" ");
        });

        // The vote costs the initiator a joker whether or not it carried.
        this.initiator.spendJoker();
        if (skipItem) {
            this.assignment.skipAll(this.initiator, this.settings.isSettingEnabled(GameSetting.RUN));
        }

        this.votedMaterial = null;
        this.voteTask = null;
    }

    public void cancelVote() {
        if (this.voteTask != null) this.voteTask.cancel();
        this.voteInProgress = false;
        this.votedMaterial = null;
        this.initiator = null;
        this.yesVotes.clear();
        this.noVotes.clear();
        this.voteTask = null;
    }
}
