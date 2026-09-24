package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.OP;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.fairplay.ModDetections;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandCheckMods extends CustomCommand implements CustomTabCompleter {

    private final ModDetections modDetections;

    public CommandCheckMods(ModDetections modDetections) {
        super("checkmods");
        this.modDetections = modDetections;

        setUsage("[clear]");
        setDescription("List detected client mods, or clear the list");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP);
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            this.modDetections.clear();
            player.sendMessage(Text.of(Prefix.FAIR_PLAY + "<gray>Cleared all detections. Players are checked again when they reconnect."));
            return;
        }
        if (args.length > 0) {
            msgUsage(player);
            return;
        }

        List<ModDetections.Detection> detections = this.modDetections.all();
        if (detections.isEmpty()) {
            player.sendMessage(Text.of(Prefix.FAIR_PLAY + "<gray>No client mods detected."));
            return;
        }

        for (ModDetections.Detection detection : detections) {
            String offline = Bukkit.getPlayer(detection.playerId()) == null ? " <dark_gray>(offline)" : "";
            Bukkit.broadcast(Text.of(detection.message() + offline));
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        return args.length == 1 ? List.of("clear") : List.of();
    }
}
