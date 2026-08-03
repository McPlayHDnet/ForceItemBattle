package forceitembattle.commands.admin;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.manager.Gamemanager;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.model.Team;
import forceitembattle.settings.GameSetting;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Dev/testing command: force the assigned item to a specific material, and
 * optionally queue a whole row of upcoming items.
**/

public class CommandForceItem extends CustomCommand implements CustomTabCompleter {

    public CommandForceItem(ForceItemBattle plugin) {
        super(plugin, "forceitem");

        setUsage("<item> [item2] [item3] ...");
        setDescription("Dev: force the current (and upcoming) item(s)");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        if (!requireOp(player)) return;

        if (!this.plugin.getGamemanager().isMidGame()) {
            player.sendMessage(Text.of("<red>The game is not running. Start it first with /start"));
            return;
        }

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

        ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
        if (forceItemPlayer == null || forceItemPlayer.isSpectator()) {
            player.sendMessage(Text.of("<red>You need to be an active player to force an item"));
            return;
        }

        Gamemanager gamemanager = this.plugin.getGamemanager();

        // Queue everything after the second item; generateMaterial() drains this
        // as new items are handed out, so the row is walked through in order.
        gamemanager.getForcedItemQueue().clear();
        if (row.size() > 2) {
            gamemanager.getForcedItemQueue().addAll(row.subList(2, row.size()));
        }

        Material current = row.get(0);
        // Second item forced when given; otherwise generate normally (queue is empty here).
        Material next = row.size() >= 2 ? row.get(1) : gamemanager.generateMaterial();

        if (this.plugin.getSettings().isSettingEnabled(GameSetting.TEAM)) {
            Team team = forceItemPlayer.currentTeam();
            team.setCurrentMaterial(current);
            team.setNextMaterial(next);
        } else {
            forceItemPlayer.setCurrentMaterial(current);
            forceItemPlayer.setNextMaterial(next);
        }

        this.plugin.getTimerManager().sendActionBar();
        this.plugin.getScoreboardManager().updateAllPlayers();

        StringBuilder confirmation = new StringBuilder("<gray>Forced item <dark_gray>» <green>"
                + CustomMaterials.nameOf(current));
        if (row.size() > 1) {
            List<String> upcoming = new ArrayList<>();
            for (Material material : row.subList(1, row.size())) {
                upcoming.add(CustomMaterials.nameOf(material));
            }
            confirmation.append(" <gray>then <white>").append(String.join("<gray>, <white>", upcoming));
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
