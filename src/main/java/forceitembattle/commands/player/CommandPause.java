package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;

public final class CommandPause extends CustomCommand {

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

        if (!this.plugin.getRoundPhase().roundRunning()) {
            player.sendMessage(Text.of("<red>The timer is already paused."));
            return;
        }
        Bukkit.broadcast(Text.of("<gold>The game has been paused!"));

        World overworld = Dimension.OVERWORLD.world();
        if (overworld != null) {
            overworld.setGameRule(GameRules.ADVANCE_TIME, false);
            overworld.setGameRule(GameRules.ADVANCE_WEATHER, false);
        }

        this.plugin.getGamemanager().pauseGame();
    }
}
