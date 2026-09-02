package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.Precondition;
import forceitembattle.gui.SettingsInventory;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSettings;
import java.util.List;
import org.bukkit.entity.Player;

public final class CommandSettings extends CustomCommand {

    private final Roster roster;
    private final GameSettings settings;

    public CommandSettings(Roster roster, GameSettings settings) {
        super("settings");
        this.roster = roster;
        this.settings = settings;

        setDescription("Manage settings");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        new SettingsInventory(this.roster, this.settings, null).open(player);
    }
}
