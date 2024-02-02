package forceitembattle.commands;

import forceitembattle.ForceItemBattle;
import forceitembattle.util.DescriptionItem;
import forceitembattle.util.RecipeInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandInfo implements CommandExecutor {

    private ForceItemBattle forceItemBattle;

    public CommandInfo(ForceItemBattle forceItemBattle) {
        this.forceItemBattle = forceItemBattle;
        this.forceItemBattle.getCommand("info").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) return false;

        ItemStack item;
        if (this.forceItemBattle.getGamemanager().isMidGame()) {
            item = new ItemStack(this.forceItemBattle.getGamemanager().getCurrentMaterial(player));
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage("§cYou need to hold an item in your hand!");
            return false;
        }

        DescriptionItem descriptionItem;
        if(this.forceItemBattle.getItemDifficultiesManager().isItemInDescriptionList(item.getType())) {
            descriptionItem = this.forceItemBattle.getItemDifficultiesManager().getDescriptionItems().get(item.getType());
            if (descriptionItem.lines() != null) {
                this.forceItemBattle.getItemDifficultiesManager().getDescriptionItemLines(descriptionItem.material()).forEach(player::sendMessage);
            } else {
                throw new NullPointerException("The item description is either null or empty");
            }
        }


        this.forceItemBattle.getRecipeInventory().showRecipe(player, item);

        return false;
    }

}
