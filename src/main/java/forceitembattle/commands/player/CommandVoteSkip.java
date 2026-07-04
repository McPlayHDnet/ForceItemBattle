package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.ForceItemPlayer;
import forceitembattle.util.Text;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class CommandVoteSkip extends CustomCommand {

    private final MiniMessage miniMessage = Text.mm();

    public CommandVoteSkip(ForceItemBattle plugin) {
        super(plugin, "voteskip");
        setDescription("Voting for a skip item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(this.miniMessage.deserialize("<red>You can only use this mid-game!"));
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.RUN)) {
            player.sendMessage(this.miniMessage.deserialize("<red>You can only start a vote when the battle `RUN` mode is enabled!"));
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (forceItemPlayer.remainingJokers() == 0 || (forceItemPlayer.currentTeam() != null && forceItemPlayer.currentTeam().getRemainingJokers() == 0)) {
            player.sendMessage(this.miniMessage.deserialize("<red>You dont have any jokers to vote!"));
            return;
        }

        if (this.plugin.getVoteSkipManager().isVoteInProgress()) {
            player.sendMessage(this.miniMessage.deserialize("<red>A skip vote is currently in progress."));
            return;
        }

        this.plugin.getVoteSkipManager().startVoting(player);
    }
}