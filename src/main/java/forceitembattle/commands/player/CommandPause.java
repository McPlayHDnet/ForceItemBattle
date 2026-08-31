package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import static forceitembattle.commands.Precondition.OP_WHEN_EVENT;
import forceitembattle.commands.Precondition;
import java.util.List;
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
    protected List<Precondition> preconditions() {
        return List.of(OP_WHEN_EVENT,
                ROUND_RUNNING.refusing("<red>The timer is already paused."));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        Bukkit.broadcast(Text.of("<gold>The game has been paused!"));

        World overworld = Dimension.OVERWORLD.world();
        if (overworld != null) {
            overworld.setGameRule(GameRules.ADVANCE_TIME, false);
            overworld.setGameRule(GameRules.ADVANCE_WEATHER, false);
        }

        this.plugin.getGamemanager().pauseGame();
    }
}
