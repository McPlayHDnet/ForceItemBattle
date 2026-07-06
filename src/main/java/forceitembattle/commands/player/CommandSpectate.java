package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class CommandSpectate extends CustomCommand {

    public CommandSpectate(ForceItemBattle plugin) {
        super(plugin, "spectate");
        setDescription("Toggle gamemode spectator");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getTimerManager().getTimeLeft() > 0) {
            player.sendMessage(Text.of("<red>This command can only be used after the game end."));
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(Text.of("<gray>You are <red>no longer<gray> spectating."));
            player.setGameMode(GameMode.CREATIVE);
        }
    }
}
