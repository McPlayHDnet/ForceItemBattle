package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;

public class CommandVoteSkip extends CustomCommand {


    public CommandVoteSkip(ForceItemBattle plugin) {
        super(plugin, "voteskip");
        setDescription("Voting for a skip item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(Text.of("<red>You can only use this mid-game!"));
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.RUN)) {
            player.sendMessage(Text.of("<red>You can only start a vote when the battle `RUN` mode is enabled!"));
            return;
        }

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());

        if (forceItemPlayer.getRemainingJokers() == 0) {
            player.sendMessage(Text.of("<red>You dont have any jokers to vote!"));
            return;
        }

        if (this.plugin.getVoteSkipManager().isVoteInProgress()) {
            player.sendMessage(Text.of("<red>A skip vote is currently in progress."));
            return;
        }

        this.plugin.getVoteSkipManager().startVoting(player);
    }
}
