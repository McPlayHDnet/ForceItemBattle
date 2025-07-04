package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandSkip extends CustomCommand {

    public CommandSkip() {
        super("skip");

        setDescription("Skip current item for player");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if(player.isOp()) {
            if (!this.plugin.getGamemanager().isMidGame()) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>The game is not running. Start it first with /start"));
                return;
            }

            if (args.length != 1) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Usage: /skip <player_name>"));
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target != null) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gray>Skipped this item for " + target.getName()));
                //this.forceItemBattle.logToFile("[" + this.forceItemBattle.getTimer().getTime() + "] | " + args[0] + " skipped " + this.forceItemBattle.getGamemanager().getCurrentMaterial(Bukkit.getPlayer(args[0])));
                this.plugin.getGamemanager().forceSkipItem(target, true);
            } else {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>This player is not online"));
            }
        }

    }
}
