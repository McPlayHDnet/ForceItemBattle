package forceitembattle.commands.player;

import forceitembattle.util.Text;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import org.bukkit.entity.Player;

public class CommandBp extends CustomCommand {

    public CommandBp(ForceItemBattle plugin) {
        super(plugin, "bp");
        setDescription("Open your backpack");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(Text.of("<red>The game has not started yet!"));
            return;
        }

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.BACKPACK)) {
            this.plugin.getBackpack().openPlayerBackpack(player);
        } else {
            player.sendMessage(Text.of("<red>Backpacks are disabled in this round!"));
        }
    }
}
