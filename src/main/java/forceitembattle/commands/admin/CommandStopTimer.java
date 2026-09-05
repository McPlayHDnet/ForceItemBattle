package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.TimerManager;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandStopTimer extends CustomCommand {

    private final TimerManager timerManager;

    public CommandStopTimer(TimerManager timerManager) {
        super("stoptimer");
        this.timerManager = timerManager;

        setDescription("Stop the timer and end the game");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP, ROUND_RUNNING);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        this.timerManager.setTimeLeft(1);
    }
}
