package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.util.DescriptionItem;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandInfo extends CustomCommand {

    public CommandInfo() {
        super("info");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = null;
        if (this.forceItemBattle.getGamemanager().isMidGame()) {
            if(this.forceItemBattle.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.forceItemBattle.getGamemanager().getForceItemPlayer(player.getUniqueId());
                item = new ItemStack(forceItemPlayer.currentMaterial());
            } else {
                player.sendMessage("§cYou are not playing.");
            }
        } else {
            item = player.getInventory().getItemInMainHand();
        }

        if (item == null) {
            return;
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage("§cYou need to hold an item in your hand!");
            return;
        }

        DescriptionItem descriptionItem;
        if (this.forceItemBattle.getItemDifficultiesManager().isItemInDescriptionList(item.getType())) {
            descriptionItem = this.forceItemBattle.getItemDifficultiesManager().getDescriptionItems().get(item.getType());
            if (descriptionItem.lines() != null) {
                this.forceItemBattle.getItemDifficultiesManager().getDescriptionItemLines(descriptionItem.material()).forEach(player::sendMessage);
            } else {
                throw new NullPointerException("The item description is either null or empty");
            }
        }

        this.forceItemBattle.getRecipeManager().createRecipeViewer(player, item);
    }

}
