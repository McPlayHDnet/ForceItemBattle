package forceitembattle.commands.player;

import static forceitembattle.commands.Precondition.PAUSED;
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

public final class CommandResume extends CustomCommand {

    public CommandResume(ForceItemBattle plugin) {
        super(plugin, "resume");
        setDescription("Resume the game");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP_WHEN_EVENT,
                PAUSED.refusing("<red>The timer is not paused!"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        Bukkit.broadcast(Text.of("<gold>The timer has been resumed!"));

        World overworld = Dimension.OVERWORLD.world();
        if (overworld != null) {
            overworld.setGameRule(GameRules.ADVANCE_TIME, true);
            overworld.setGameRule(GameRules.ADVANCE_WEATHER, true);
        }

        this.plugin.getGamemanager().resumeGame();
    }
}
