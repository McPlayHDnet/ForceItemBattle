package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
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

    private final ForceItemBattle plugin;
    private final Set<UUID> yesVotes = new HashSet<>();
    private final Set<UUID> noVotes = new HashSet<>();
    private final Random random = new Random();
    @Getter
    private boolean voteInProgress = false;
    private BukkitTask voteTask;
    private Material votedMaterial;
    private ForceItemPlayer initiator;

    public VoteSkipManager(ForceItemBattle plugin) {
        this.plugin = plugin;
    }

    @Override
    public void disable() {
        if (this.voteTask != null) {
            this.voteTask.cancel();
            this.voteTask = null;
        }
    }

    public void startVoting(Player initiator) {
        this.voteInProgress = true;
        this.yesVotes.clear();
        this.noVotes.clear();
        this.yesVotes.add(initiator.getUniqueId());
        this.initiator = this.plugin.getGamemanager().getForceItemPlayer(initiator.getUniqueId());
        this.votedMaterial = this.initiator.activeMaterial();

        String materialName = CustomMaterials.nameOf(this.votedMaterial);
        String unicodeMaterial = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, this.votedMaterial);

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

        int totalPlayers = this.plugin.getGamemanager().forceItemPlayerMap().size();
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
        String unicodeMaterial = this.plugin.getItemDifficultiesManager().getUnicodeFromMaterial(true, this.votedMaterial);

        boolean skipItem = false;
        boolean isTie = yes == no;

        if (yes > no) {
            skipItem = true;
        } else if (isTie) {
            skipItem = random.nextBoolean();
        }

        boolean finalSkipItem = skipItem;
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(Text.of("<gray>The skip voting has been ended."));
            player.sendMessage(Text.of("  <dark_gray>● <green><b>YES</b> <dark_gray>» <gold>" + yes + " " + voteLabel));
            player.sendMessage(Text.of("  <dark_gray>● <red><b>NO</b> <dark_gray>» <gold>" + no + " " + voteLabel));
            player.sendMessage(" ");
            if (isTie) {
                player.sendMessage(Text.of("<gray>It was a tie! Choosing randomly..."));
            }
            player.sendMessage(Text.of("<dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName + " <gray>is " + (finalSkipItem ? "now" : "not") + " skipped."));
            player.sendMessage(" ");
        });

        // The vote costs the initiator a joker whether or not it carried.
        this.initiator.spendJoker();
        if (skipItem) {
            this.plugin.getGamemanager().forceSkipItem(this.initiator.player());
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
