package forceitembattle.commands.admin;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

public class CommandPause extends CustomCommand {

    public CommandPause() {
        super("pause");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.forceItemBattle.getGamemanager().isMidGame()) {
            player.sendMessage("§cThe timer is already paused.");
            return;
        }

        Bukkit.broadcastMessage("§6The timer has been paused!");
        Bukkit.getWorld("world").setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.forceItemBattle.getGamemanager().setCurrentGameState(GameState.PAUSED_GAME);
    }
}
