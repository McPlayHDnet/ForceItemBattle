package forceitembattle.manager;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class VoteSkipManager {

    @Getter
    private boolean voteInProgress = false;

    private final Set<UUID> yesVotes = new HashSet<>();
    private final Set<UUID> noVotes = new HashSet<>();
    private BukkitTask voteTask;
    private Material votedMaterial;
    private ForceItemPlayer initiator;

    private final MiniMessage miniMessage = ForceItemBattle.getInstance().getGamemanager().getMiniMessage();
    private final Random random = new Random();

    public void startVoting(Player initiator) {
        this.voteInProgress = true;
        this.yesVotes.clear();
        this.noVotes.clear();
        this.yesVotes.add(initiator.getUniqueId());
        this.initiator = ForceItemBattle.getInstance().getGamemanager().getForceItemPlayer(initiator.getUniqueId());
        this.votedMaterial = this.initiator.getCurrentMaterial();

        String materialName = ForceItemBattle.getInstance().getGamemanager().getMaterialName(this.votedMaterial);
        String unicodeMaterial = ForceItemBattle.getInstance().getItemDifficultiesManager().getUnicodeFromMaterial(true, this.votedMaterial);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(" ");
            player.sendMessage(this.miniMessage.deserialize("<gray>A skip voting has been started by <green>" + initiator.getName() + "<gray>."));
            player.sendMessage(this.miniMessage.deserialize("  <dark_gray>● <gray>Duration <dark_gray>» <gold>60 seconds"));
            player.sendMessage(this.miniMessage.deserialize("  <dark_gray>● <gray>Item <dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName));
            player.sendMessage(" ");
            player.sendMessage(this.miniMessage.deserialize("                  <dark_gray>[<green><b><click:run_command:'/vote yes'>YES</click></b><dark_gray>]          <dark_gray>[<red><b><click:run_command:'/vote no'>NO</click></b><dark_gray>]"));
            player.sendMessage(" ");
        });

        this.voteTask = Bukkit.getScheduler().runTaskLater(ForceItemBattle.getInstance(), this::endVoting, 20 * 60);
    }

    public void castVote(Player player, boolean voteYes) {
        UUID uuid = player.getUniqueId();
        if (this.yesVotes.contains(uuid) || this.noVotes.contains(uuid)) {
            player.sendMessage(this.miniMessage.deserialize("<red>You have already voted."));
            return;
        }

        if (voteYes) {
            this.yesVotes.add(uuid);
            player.sendMessage(this.miniMessage.deserialize("<gray>You voted for <green><b>YES</b><gray>!"));
        } else {
            this.noVotes.add(uuid);
            player.sendMessage(this.miniMessage.deserialize("<gray>You voted for <red><b>NO</b><gray>!"));
        }

        int totalPlayers = ForceItemBattle.getInstance().getGamemanager().forceItemPlayerMap().size();
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

        String materialName = ForceItemBattle.getInstance().getGamemanager().getMaterialName(this.votedMaterial);
        String unicodeMaterial = ForceItemBattle.getInstance().getItemDifficultiesManager().getUnicodeFromMaterial(true, this.votedMaterial);

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
            player.sendMessage(this.miniMessage.deserialize("<gray>The skip voting has been ended."));
            player.sendMessage(this.miniMessage.deserialize("  <dark_gray>● <green><b>YES</b> <dark_gray>» <gold>" + yes + " " + voteLabel));
            player.sendMessage(this.miniMessage.deserialize("  <dark_gray>● <red><b>NO</b> <dark_gray>» <gold>" + no + " " + voteLabel));
            player.sendMessage(" ");
            if (isTie) {
                player.sendMessage(this.miniMessage.deserialize("<gray>It was a tie! Choosing randomly..."));
            }
            player.sendMessage(this.miniMessage.deserialize("<dark_gray>» <reset>" + unicodeMaterial + " <gold>" + materialName + " <gray>is " + (finalSkipItem ? "now" : "not") + " skipped."));
            player.sendMessage(" ");
        });

        if (skipItem) {
            this.initiator.setRemainingJokers(this.initiator.remainingJokers() - 1);
            ForceItemBattle.getInstance().getGamemanager().forceSkipItem(this.initiator.player(), false);
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
