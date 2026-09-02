package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.BackpackManager;
import forceitembattle.settings.GameSetting;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandBp extends CustomCommand {

    private final BackpackManager backpackManager;

    public CommandBp(BackpackManager backpackManager) {
        super("bp");
        this.backpackManager = backpackManager;
        setDescription("Open your backpack");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(ROUND_RUNNING.refusing("<red>The game has not started yet!"),
                Precondition.setting(GameSetting.BACKPACK, "<red>Backpacks are disabled in this round!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        this.backpackManager.openPlayerBackpack(player);
    }
}
