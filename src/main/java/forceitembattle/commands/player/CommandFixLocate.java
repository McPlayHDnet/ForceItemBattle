package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import forceitembattle.ForceItemBattle;
import forceitembattle.commands.CustomCommand;
import forceitembattle.model.Locator;
import forceitembattle.util.Prefix;
import forceitembattle.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CommandFixLocate extends CustomCommand implements TabCompleter {

    public CommandFixLocate(ForceItemBattle plugin) {
        super(plugin, "fixlocate");
        setUsage("[name|all]");
        setDescription("Dismiss your active locator boss bars and lines");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        Map<String, Locator> active = this.plugin.getLocatorManager().getActiveLocators(player);

        if (active.isEmpty()) {
            player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>You have no active locators."));
            return;
        }

        // Explicit target given (typed, or via a click in the picker).
        if (args.length > 0) {
            String query = String.join(" ", args);

            if (query.equalsIgnoreCase("all")) {
                int dismissed = this.plugin.getLocatorManager().dismissAll(player);
                player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Dismissed <dark_aqua>" + dismissed + " <gray>locator" + (dismissed == 1 ? "" : "s") + "."));
                return;
            }

            for (Map.Entry<String, Locator> entry : active.entrySet()) {
                Locator locator = entry.getValue();
                if (query.equalsIgnoreCase(entry.getKey()) || query.equalsIgnoreCase(locator.getStructureName())) {
                    this.plugin.getLocatorManager().dismiss(player, entry.getKey());
                    player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Dismissed <dark_aqua>" + locator.getStructureName() + "<gray>."));
                    return;
                }
            }

            player.sendMessage(Text.of(Prefix.LOCATOR + "<red>You have no active <dark_aqua>" + query + " <red>locator."));
            this.sendPicker(player, active);
            return;
        }

        // No argument: one active -> just dismiss it; several -> let the player choose.
        if (active.size() == 1) {
            Map.Entry<String, Locator> only = active.entrySet().iterator().next();
            this.plugin.getLocatorManager().dismiss(player, only.getKey());
            player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>Dismissed <dark_aqua>" + only.getValue().getStructureName() + "<gray>."));
            return;
        }

        this.sendPicker(player, active);
    }

    private void sendPicker(Player player, Map<String, Locator> active) {
        player.sendMessage(Text.of(Prefix.LOCATOR + "<gray>You have <dark_aqua>" + active.size() + " <gray>active locators — pick which to dismiss:"));

        for (Map.Entry<String, Locator> entry : active.entrySet()) {
            Locator locator = entry.getValue();
            player.sendMessage(Text.of("<dark_gray>  • <dark_aqua>" + locator.getStructureName()
                    + " <click:run_command:'/fixlocate " + Text.tagArgument(entry.getKey()) + "'>"
                    + "<hover:show_text:'<gray>Dismiss <dark_aqua>" + Text.tagArgument(locator.getStructureName()) + "'>"
                    + "<red>[✖ Dismiss]</red></hover></click>"));
        }

        player.sendMessage(Text.of("<dark_gray>  ⇒ <click:run_command:'/fixlocate all'>"
                + "<hover:show_text:'<gray>Dismiss every active locator'>"
                + "<red>[✖ Dismiss all]</red></hover></click>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!(sender instanceof Player player) || args.length != 1) {
            return suggestions;
        }

        String partial = args[0].toLowerCase();
        for (String structureId : this.plugin.getLocatorManager().getActiveLocators(player).keySet()) {
            if (structureId.toLowerCase().startsWith(partial)) {
                suggestions.add(structureId);
            }
        }
        if ("all".startsWith(partial)) {
            suggestions.add("all");
        }
        return suggestions;
    }
}
