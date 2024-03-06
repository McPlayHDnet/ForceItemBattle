package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import org.bukkit.entity.Player;

public class CommandBp extends CustomCommand {

    public CommandBp() {
        super("bp");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage("§cThe game has not started yet!");
            return;
        }

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
            this.plugin.getBackpack().openPlayerBackpack(player);
        } else {
            player.sendMessage("§cBackpacks are disabled in this round!");
        }
    }
}
