package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandResume implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandResume(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("resume").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        if(!this.forceItemBattle.getGamemanager().isPausedGame()) {
            player.sendMessage("§cThe timer is not paused!");
            return false;
        }

        Bukkit.broadcastMessage("§6The timer has been resumed!");
        this.forceItemBattle.getGamemanager().setCurrentGameState(GameState.MID_GAME);
        return false;
    }
}
