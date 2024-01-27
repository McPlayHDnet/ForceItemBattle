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

public class CommandInfoWiki implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        if(ForceItemBattle.getTimer().isRunning()) {
            Material currentMaterial = ForceItemBattle.getGamemanager().getCurrentMaterial(player);

            TextComponent infoWikiBefore = new TextComponent("§7Check out the minecraft wiki for §a" + WordUtils.capitalize(currentMaterial.name().toLowerCase().replace("_", " ") + " "));
            TextComponent infoWikiAfter = new TextComponent("§f[§bClick here§f]");
            infoWikiAfter.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.wiki/" + currentMaterial.name().toLowerCase()));

            player.spigot().sendMessage(infoWikiBefore, infoWikiAfter);
        }
        return false;
    }
}
