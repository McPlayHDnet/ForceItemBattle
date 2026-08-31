package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandSkip extends CustomCommand {

    public CommandSkip(ForceItemBattle plugin) {
        super(plugin, "skip");

        setDescription("Skip current item for player");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP, ROUND_RUNNING);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {

        if (args.length != 1) {
            player.sendMessage(Text.of("<red>Usage: /skip <player_name>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target != null) {
            player.sendMessage(Text.of("<gray>Skipped this item for " + target.getName()));
            this.plugin.getGamemanager().forceSkipItem(target);
        } else {
            player.sendMessage(Text.of("<red>This player is not online"));
        }


    }
}
