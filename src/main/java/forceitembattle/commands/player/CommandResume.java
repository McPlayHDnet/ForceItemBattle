package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

public class CommandResume extends CustomCommand {

    public CommandResume() {
        super("resume");
        setDescription("Resume the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT) && !player.isOp()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You don't have permission to use this command."));
            return;
        }

        if (!this.plugin.getGamemanager().isPausedGame()) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>The timer is not paused!"));
            return;
        }

        Bukkit.broadcast(this.plugin.getGamemanager().getMiniMessage().deserialize("<gold>The timer has been resumed!"));
        Bukkit.getWorld("world").setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        this.plugin.getGamemanager().setCurrentGameState(GameState.MID_GAME);
    }
}
