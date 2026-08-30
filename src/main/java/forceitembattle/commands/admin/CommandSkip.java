package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandSkip extends CustomCommand {

    public CommandSkip(ForceItemBattle plugin) {
        super(plugin, "skip");

        setDescription("Skip current item for player");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) {
            if (!this.plugin.getGamemanager().roundRunning()) {
                player.sendMessage(Text.of("<red>The game is not running. Start it first with /start"));
                return;
            }

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
}
