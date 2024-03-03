package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.settings.GameSetting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBp implements CommandExecutor {

    private final ForceItemBattle forceItemBattle;

    public CommandBp(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("bp").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player player) {
            if (this.forceItemBattle.getGamemanager().isMidGame()) {
                if (this.forceItemBattle.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
                    this.forceItemBattle.getBackpack().openPlayerBackpack(player);
                } else {
                    player.sendMessage("§cBackpacks are disabled in this round!");
                }
            } else {
                player.sendMessage("§cThe game has not started yet!");
            }
        }

        return false;
    }
}
