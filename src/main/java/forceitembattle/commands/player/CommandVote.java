package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.settings.GameSetting;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CommandVote extends CustomCommand implements CustomTabCompleter {

    private final MiniMessage miniMessage = ForceItemBattle.getInstance().getGamemanager().getMiniMessage();

    public CommandVote() {
        super("vote");
        setDescription("Voting for a skip item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(this.miniMessage.deserialize("<red>You can only use this mid-game!"));
            return;
        }

        if (!this.plugin.getSettings().isSettingEnabled(GameSetting.RUN)) {
            player.sendMessage(this.miniMessage.deserialize("<red>You can only use vote when the battle `RUN` mode is enabled!"));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(this.miniMessage.deserialize("<gray>Usage: <yellow>/vote <green>yes</green>|<red>no</red>"));
            return;
        }

        if (!ForceItemBattle.getInstance().getVoteSkipManager().isVoteInProgress()) {
            player.sendMessage(this.miniMessage.deserialize("<red>No skip vote is currently in progress."));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "yes" -> ForceItemBattle.getInstance().getVoteSkipManager().castVote(player, true);
            case "no"  -> ForceItemBattle.getInstance().getVoteSkipManager().castVote(player, false);
            case "cancel" -> {
                if (!player.isOp()) {
                    player.sendMessage(this.miniMessage.deserialize("<red>You must be an operator to cancel a vote."));
                    return;
                }

                ForceItemBattle.getInstance().getVoteSkipManager().cancelVote();
                player.sendMessage(this.miniMessage.deserialize("<gray>You cancelled the vote."));
                Bukkit.getOnlinePlayers().forEach(p ->
                        p.sendMessage(this.miniMessage.deserialize("<red><b>The vote has been cancelled by an operator!</b>"))
                );
            }
            default -> player.sendMessage(this.miniMessage.deserialize("<red>Invalid vote option. Use <yellow>/vote yes</yellow> or <yellow>/vote no</yellow>."));
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
