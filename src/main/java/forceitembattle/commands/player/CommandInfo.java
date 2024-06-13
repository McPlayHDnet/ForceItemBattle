package forceitembattle.commands.player;

import forceitembattle.commands.CustomCommand;
import forceitembattle.commands.CustomTabCompleter;
import forceitembattle.util.DescriptionItem;
import forceitembattle.util.ForceItemPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandInfo extends CustomCommand implements CustomTabCompleter {

    public CommandInfo() {
        super("info");
        setUsage("[item]");
        setDescription("Get information about an item");
    }

    @Override
    public void onPlayerCommand(Player player, String label, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (args.length == 1) {
            Material material = Material.matchMaterial(args[0]);
            if (material == null) {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>Invalid item name"));
                return;
            }
            item = new ItemStack(material);

        } else if (this.plugin.getGamemanager().isMidGame()) {
            if (this.plugin.getGamemanager().forceItemPlayerExist(player.getUniqueId())) {
                ForceItemPlayer forceItemPlayer = this.plugin.getGamemanager().getForceItemPlayer(player.getUniqueId());
                item = new ItemStack(forceItemPlayer.currentTeam() == null ? forceItemPlayer.currentMaterial() : forceItemPlayer.currentTeam().getCurrentMaterial());
            } else {
                player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You are not playing, type /info [item] to get information about an item"));
            }
        }

        if (item.getType() == Material.AIR) {
            player.sendMessage(this.plugin.getGamemanager().getMiniMessage().deserialize("<red>You need to hold an item in your hand!"));
            return;
        }

        DescriptionItem descriptionItem;
        if (this.plugin.getItemDifficultiesManager().itemHasDescription(item.getType())) {
            descriptionItem = this.plugin.getItemDifficultiesManager().getDescriptionItems().get(item.getType());
            if (descriptionItem.lines() != null) {
                this.plugin.getItemDifficultiesManager().getDescriptionItemLines(descriptionItem.material()).forEach(player::sendMessage);
            } else {
                throw new NullPointerException("The item description is either null or empty");
            }
        }

        this.plugin.getRecipeManager().createRecipeViewer(player, item);
    }

    private static final List<String> MATERIALS = Arrays.stream(Material.values())
            .map(material -> material.name().toLowerCase())
            .sorted()
            .toList();

    @Override
    public List<String> onTabComplete(Player player, String label, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String key = args[0].toLowerCase();
        int index = Collections.binarySearch(MATERIALS, key);
        if (index < 0) {
            index = -index - 1;
        }

        List<String> result = new ArrayList<>();
        while (index < MATERIALS.size()) {
            String s = MATERIALS.get(index);
            if (s.startsWith(key)) {
                result.add(s);
            } else {
                break;
            }
            index++;
        }
        return result;
    }
}
