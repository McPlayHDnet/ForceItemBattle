package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.ForceItemPlayer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
                item = new ItemStack(forceItemPlayer.currentMaterial());
            } else {
                player.sendMessage("§cYou are not playing.");
                return;
            }
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage("§cYou need to hold an item in your hand!");
            return;
        }

        TextComponent infoWikiBefore = new TextComponent("§7Check out the minecraft wiki for §a" + WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " ") + " "));
        TextComponent infoWikiAfter = new TextComponent("§f[§bClick here§f]");
        infoWikiAfter.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.wiki/" + this.plugin.getGamemanager().formatMaterialName(item.getType().name().toLowerCase())));

        player.spigot().sendMessage(infoWikiBefore, infoWikiAfter);

    }
}
