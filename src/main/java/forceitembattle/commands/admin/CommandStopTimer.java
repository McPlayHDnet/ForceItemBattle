package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;

public final class CommandStopTimer extends CustomCommand {

    public CommandStopTimer(ForceItemBattle plugin) {
        super(plugin, "stoptimer");

        setDescription("Stop the timer and end the game");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP, ROUND_RUNNING);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        this.plugin.getTimerManager().setTimeLeft(1);
    }
}
