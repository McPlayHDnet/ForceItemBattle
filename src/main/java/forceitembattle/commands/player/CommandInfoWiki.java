package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.ForceItemPlayer;
import org.apache.commons.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandInfoWiki extends CustomCommand {

    public CommandInfoWiki() {
        super("infowiki");
        setDescription("Get wiki info link for your current item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();;

        if (this.plugin.getGamemanager().isMidGame()) {
            if (this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                if(forceItemPlayer.isSpectator()) {
                    player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You are not playing."));
                    return;
                }
                item = new ItemStack(forceItemPlayer.getCurrentMaterial());
            } else {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You are not playing."));
                return;
            }
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You need to hold an item in your hand!"));
            return;
        }

        player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize(
                "<gray>Check out the minecraft wiki for <green>" + WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " ")
                    + " <click:open_url:https://minecraft.wiki/" + this.plugin.getGamemanager().formatMaterialName(item.getType().name().toLowerCase()) + "><white>[<aqua>Click here<white>]"))
        );

    }
}
