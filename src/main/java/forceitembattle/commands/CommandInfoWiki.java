package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.ForceItemPlayer;
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

    private final ForceItemBattle forceItemBattle;

    public CommandInfoWiki(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("infowiki").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        ItemStack item = null;
        if (this.forceItemBattle.getGamemanager().isMidGame()) {
            if (this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());
                item = new ItemStack(forceItemPlayer.getCurrentMaterial());
            } else {
                player.sendMessage("§cYou are not playing.");
            }
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        if (item == null) return false;

        if (item.getType() == Material.AIR) {
            player.sendMessage("§cYou need to hold an item in your hand!");
            return false;
        }

        TextComponent infoWikiBefore = new TextComponent("§7Check out the minecraft wiki for §a" + WordUtils.capitalizeFully(item.getType().name().toLowerCase().replace("_", " ") + " "));
        TextComponent infoWikiAfter = new TextComponent("§f[§bClick here§f]");
        infoWikiAfter.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.wiki/" + this.forceItemBattle.getGamemanager().formatMaterialName(item.getType().name().toLowerCase())));

        player.spigot().sendMessage(infoWikiBefore, infoWikiAfter);

        return false;
    }
}
