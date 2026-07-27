package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.model.Dimension;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;

public class CommandResume extends CustomCommand {

    public CommandResume(ForceItemBattle plugin) {
        super(plugin, "resume");
        setDescription("Resume the game");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.plugin.getSettings().isSettingEnabled(GameSetting.EVENT) && !player.isOp()) {
            player.sendMessage(Text.of("<red>You don't have permission to use this command."));
            return;
        }

        if (!this.plugin.getGamemanager().isPausedGame()) {
            player.sendMessage(Text.of("<red>The timer is not paused!"));
            return;
        }

        Bukkit.broadcast(Text.of("<gold>The timer has been resumed!"));
        Dimension.OVERWORLD.world().setGameRule(GameRules.ADVANCE_TIME, true);
        this.plugin.getGamemanager().resumeGame();
    }
}
