package forceitembattle.commands.admin;

import static forceitembattle.commands.Precondition.PARTICIPANT;
import static forceitembattle.commands.Precondition.OP;
import static forceitembattle.commands.Precondition.ROUND_RUNNING;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.commands.Precondition;
import forceitembattle.manager.ForceItemAssignment;
import forceitembattle.manager.ScoreboardManager;
import forceitembattle.manager.TimerManager;
import forceitembattle.model.CustomMaterials;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Roster;
import forceitembattle.settings.GameSetting;
import forceitembattle.settings.GameSettings;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Dev/testing command: force the assigned item to a specific material, and
 * optionally queue a whole row of upcoming items.
**/

public final class CommandForceItem extends CustomCommand implements CustomTabCompleter {

    private final ForceItemAssignment assignment;
    private final GameSettings settings;
    private final TimerManager timerManager;
    private final Roster roster;
    private final ScoreboardManager scoreboardManager;

    public CommandForceItem(ForceItemAssignment assignment, GameSettings settings, TimerManager timerManager, Roster roster, ScoreboardManager scoreboardManager) {
        super("forceitem");
        this.assignment = assignment;
        this.settings = settings;
        this.timerManager = timerManager;
        this.roster = roster;
        this.scoreboardManager = scoreboardManager;

        setUsage("<item> [item2] [item3] ...");
        setDescription("Dev: force the current (and upcoming) item(s)");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of(OP, ROUND_RUNNING,
                PARTICIPANT.refusing("<red>You need to be an active player to force an item"));
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Text.of("<red>Usage: /forceitem <item> [item2] [item3] ..."));
            return;
        }

        // Parse and validate every argument up front so we never apply a partial row.
        List<Material> row = new ArrayList<>();
        for (String arg : args) {
            Material material = Material.matchMaterial(arg);
            if (material == null || material.isLegacy() || !material.isItem()) {
                player.sendMessage(Text.of("<red>Unknown item: <white>" + arg));
                return;
            }
            row.add(material);
        }

        // Present because PARTICIPANT is declared above; the gate already refused anyone else.
        ForceItemPlayer forceItemPlayer =
                this.roster.participant(player.getUniqueId()).orElseThrow();

        // The whole row in one call: first item now, second queued behind it, the rest drained in
        // order as they are found. The queue is the assignment module's, keyed by this owner --
        // it used to be a server-wide deque this command reached into, so a row forced here was
        // drained by whichever player found something next.
        this.assignment.force(forceItemPlayer.scoreOwner(), row,
                this.settings.isSettingEnabled(GameSetting.RUN));

        Material current = row.getFirst();

        this.timerManager.sendActionBar();
        this.scoreboardManager.updateAllPlayers();

        StringBuilder confirmation = new StringBuilder("<gray>Forced item <dark_gray>» <green>"
                + CustomMaterials.nameOf(current));
        if (row.size() > 1) {
            confirmation.append(" <gray>then <white>").append(row.subList(1, row.size()).stream()
                    .map(CustomMaterials::nameOf)
                    .collect(Collectors.joining("<gray>, <white>")));
        }
        player.sendMessage(Text.of(confirmation.toString()));
    }

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 0) {
            return new ArrayList<>();
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isItem()) {
                continue;
            }
            String name = material.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}
