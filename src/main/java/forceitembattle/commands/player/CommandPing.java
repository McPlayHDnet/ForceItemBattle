package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.commands.CustomCommand;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;

public final class CommandPing extends CustomCommand {

    public CommandPing() {
        super("ping");
        setDescription("Check your ping");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        player.sendMessage(Text.of("<green>Your ping: <yellow>" + player.getPing() + "ms"));

    }
}
