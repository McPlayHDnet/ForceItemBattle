package forceitembattle.commands.player;

import forceitembattle.ForceItemBattle;
import forceitembattle.model.CustomMaterials;
import forceitembattle.commands.CustomCommand;
import forceitembattle.model.ForceItemPlayer;
import forceitembattle.util.Text;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandInfoWiki extends CustomCommand {

    public CommandInfoWiki(ForceItemBattle plugin) {
        super(plugin, "infowiki");
        setDescription("Get wiki info link for your current item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ;

        if (this.plugin.getGamemanager().isMidGame()) {
            if (this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                if (forceItemPlayer.isSpectator()) {
                    player.sendMessage(Text.of("<red>You are not playing."));
                    return;
                }
                item = new ItemStack(forceItemPlayer.activeMaterial());
            } else {
                player.sendMessage(Text.of("<red>You are not playing."));
                return;
            }
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage(Text.of("<red>You need to hold an item in your hand!"));
            return;
        }

        player.sendMessage(Text.of(
                "<gray>Check out the minecraft wiki for <green>" + WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " ")
                        + " <click:open_url:https://minecraft.wiki/" + CustomMaterials.wikiSlugOf(item.getType()) + "><white>[<aqua>Click here<white>]"))
        );

    }
}
