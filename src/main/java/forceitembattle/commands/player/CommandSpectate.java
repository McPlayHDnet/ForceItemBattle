package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class CommandSpectate extends CustomCommand {

    public CommandSpectate() {
        super("spectate");
        setDescription("Toggle gamemode spectator");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT) && !player.isOp()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You don't have permission to use this command."));
            return;
        }

        if (this.plugin.getTimer().getTimeLeft() > 0) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>This command can only be used after the game end."));
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<gray>You are <red>no longer<gray> spectating."));
            player.setGameMode(GameMode.CREATIVE);
        }

    }
}
