package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.TimerManager;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class CommandSpectate extends CustomCommand {

    private final TimerManager timerManager;

    public CommandSpectate(TimerManager timerManager) {
        super("spectate");
        this.timerManager = timerManager;
        setDescription("Toggle gamemode spectator");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (this.timerManager.getTimeLeft() > 0) {
            player.sendMessage(Text.of("<red>This command can only be used after the game end."));
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(Text.of("<gray>You are <red>no longer<gray> spectating."));
            player.setGameMode(GameMode.CREATIVE);
        }
    }
}
