package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandInfoWiki implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        ItemStack item;
        if (ForceItemBattle.getTimer().isRunning()) {
            item = new ItemStack(ForceItemBattle.getGamemanager().getCurrentMaterial(player));
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage("§cYou need to hold an item in your hand!");
            return false;
        }

        TextComponent infoWikiBefore = new TextComponent("§7Check out the minecraft wiki for §a" + WordUtils.capitalize(item.getType().name().toLowerCase().replace("_", " ") + " "));
        TextComponent infoWikiAfter = new TextComponent("§f[§bClick here§f]");
        infoWikiAfter.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.wiki/" + item.getType().name().toLowerCase()));

        player.spigot().sendMessage(infoWikiBefore, infoWikiAfter);

        return false;
    }
}
