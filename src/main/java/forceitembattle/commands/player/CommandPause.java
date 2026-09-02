package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import static forceitembattle.commands.Precondition.OP_WHEN_EVENT;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.Dimension;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class CommandPause extends CustomCommand {

    private final Gamemanager gamemanager;

    public CommandPause(Gamemanager gamemanager) {
        super("pause");
        this.gamemanager = gamemanager;
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

        this.gamemanager.pauseGame();
    }
}
