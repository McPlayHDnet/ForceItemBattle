package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.VoteSkipManager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandVoteSkip extends CustomCommand {


    private final Roster roster;
    private final VoteSkipManager voteSkipManager;

    public CommandVoteSkip(Roster roster, VoteSkipManager voteSkipManager) {
        super("voteskip");
        this.roster = roster;
        this.voteSkipManager = voteSkipManager;
        setDescription("Voting for a skip item");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(ROUND_RUNNING.refusing("<red>You can only use this mid-game!"),
                Precondition.setting(GameSetting.RUN, "<red>You can only start a vote when the battle `RUN` mode is enabled!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ForceItemPlayer forceItemPlayer = this.roster.get(player.getUniqueId());
        if (forceItemPlayer == null) {
            player.sendMessage(Text.of("<red>You are not playing."));
            return;
        }

        if (forceItemPlayer.activeJokers() == 0) {
            player.sendMessage(Text.of("<red>You dont have any jokers to vote!"));
            return;
        }

        if (this.voteSkipManager.isVoteInProgress()) {
            player.sendMessage(Text.of("<red>A skip vote is currently in progress."));
            return;
        }

        this.voteSkipManager.startVoting(player);
    }
}
