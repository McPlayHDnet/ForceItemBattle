package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;

public final class CommandVoteSkip extends CustomCommand {


    public CommandVoteSkip(ForceItemBattle plugin) {
        super(plugin, "voteskip");
        setDescription("Voting for a skip item");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(ROUND_RUNNING.refusing("<red>You can only use this mid-game!"),
                Precondition.setting(GameSetting.RUN, "<red>You can only start a vote when the battle `RUN` mode is enabled!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ForceItemPlayer forceItemPlayer = this.plugin.getRoster().get(player.getUniqueId());
        if (forceItemPlayer == null) {
            player.sendMessage(Text.of("<red>You are not playing."));
            return;
        }

        if (forceItemPlayer.activeJokers() == 0) {
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
