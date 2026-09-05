package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.VoteSkipManager;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandVote extends CustomCommand implements CustomTabCompleter {


    private final VoteSkipManager voteSkipManager;

    public CommandVote(VoteSkipManager voteSkipManager) {
        super("vote");
        this.voteSkipManager = voteSkipManager;
        setDescription("Voting for a skip item");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(ROUND_RUNNING.refusing("<red>You can only use this mid-game!"),
                Precondition.setting(GameSetting.RUN, "<red>You can only use vote when the battle `RUN` mode is enabled!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Text.of("<gray>Usage: <yellow>/vote <green>yes</green>|<red>no</red>"));
            return;
        }

        if (!this.voteSkipManager.isVoteInProgress()) {
            player.sendMessage(Text.of("<red>No skip vote is currently in progress."));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "yes" -> this.voteSkipManager.castVote(player, true);
            case "no" -> this.voteSkipManager.castVote(player, false);
            case "cancel" -> requireOp(player, () -> {
                this.voteSkipManager.cancelVote();
                player.sendMessage(Text.of("<gray>You cancelled the vote."));
                Bukkit.getOnlinePlayers().forEach(p ->
                        p.sendMessage(Text.of("<red><b>The vote has been cancelled by an operator!</b>"))
                );
            });
            default ->
                    player.sendMessage(Text.of("<red>Invalid vote option. Use <yellow>/vote yes</yellow> or <yellow>/vote no</yellow>."));
        }

    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 1) {
            List<String> options = player.isOp()
                    ? Arrays.asList("yes", "no", "cancel")
                    : Arrays.asList("yes", "no");

            return options.stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
