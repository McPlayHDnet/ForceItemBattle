package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.model.RoundPhase;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class CommandSpectate extends CustomCommand {

    private final RoundPhase roundPhase;

    public CommandSpectate(RoundPhase roundPhase) {
        super("spectate");
        this.roundPhase = roundPhase;
        setDescription("Toggle gamemode spectator");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        // The phase, not the clock. "After the game end" is a phase question, and the counter
        // answered it by accident: it is loaded from config before a round has ever been played and
        // frozen above zero during a pause, so it agreed only because nothing else set it to zero.
        if (!this.roundPhase.isEndGame()) {
            player.sendMessage(Text.of("<red>This command can only be used after the game end."));
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(Text.of("<gray>You are <red>no longer<gray> spectating."));
            player.setGameMode(GameMode.CREATIVE);
        }
    }
}
