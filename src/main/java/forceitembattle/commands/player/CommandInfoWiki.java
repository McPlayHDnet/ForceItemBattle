package forceitembattle.commands.player;

import forceitembattle.commands.Precondition;
import java.util.List;
import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.commands.CustomCommand;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import org.apache.commons.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CommandInfoWiki extends CustomCommand {

    public CommandInfoWiki(ForceItemBattle plugin) {
        super(plugin, "infowiki");
        setDescription("Get wiki info link for your current item");
    }

    @Override
    protected List<Precondition> preconditions() {
        return List.of();
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (this.plugin.getRoundPhase().roundRunning()) {
            ForceItemPlayer forceItemPlayer =
                    this.plugin.getRoster().participant(player.getUniqueId()).orElse(null);
            if (forceItemPlayer == null) {
                player.sendMessage(Text.of("<red>You are not playing."));
                return;
            }
            // During a round the wiki link is for the force item, not the held one.
            item = new ItemStack(forceItemPlayer.activeMaterial());
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage(Text.of("<red>You need to hold an item in your hand!"));
            return;
        }

        // capitalizeFully covers the item name and nothing else. It used to close after the whole
        // string: it lowercased the slug CustomMaterials.wikiSlugOf had just built -- so every
        // multi-word item linked to a broken page, minecraft.wiki being case-sensitive past the
        // first letter -- and turned "Click here" into "click Here".
        player.sendMessage(Text.of(
                "<gray>Check out the minecraft wiki for <green>"
                        + WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " "))
                        + " <click:open_url:https://minecraft.wiki/" + CustomMaterials.wikiSlugOf(item.getType())
                        + "><white>[<aqua>Click here<white>]"));

    }
}
