package forceitembattle.commands.player;

import forceitembattle.util.Text;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;

public class CommandPause extends CustomCommand {

    public CommandPause(ForceItemBattle plugin) {
        super(plugin, "pause");
        setDescription("Pause the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT) && !player.isOp()) {
            player.sendMessage(Text.of("<red>You don't have permission to use this command."));
            return;
        }

        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(Text.of("<red>The timer is already paused."));
            return;
        }
        Bukkit.broadcast(Text.of("<gold>The game has been paused!"));
        Bukkit.getWorld("world").setGameRule(GameRules.ADVANCE_TIME, false);
        this.plugin.getGamemanager().setCurrentGameState(GameState.PAUSED_GAME);
    }
}
