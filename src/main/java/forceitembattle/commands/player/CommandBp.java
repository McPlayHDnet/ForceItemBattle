package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import org.bukkit.entity.Player;

public final class CommandBp extends CustomCommand {

    public CommandBp(ForceItemBattle plugin) {
        super(plugin, "bp");
        setDescription("Open your backpack");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(ROUND_RUNNING.refusing("<red>The game has not started yet!"),
                Precondition.setting(GameSetting.BACKPACK, "<red>Backpacks are disabled in this round!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        this.plugin.getBackpackManager().openPlayerBackpack(player);
    }
}
